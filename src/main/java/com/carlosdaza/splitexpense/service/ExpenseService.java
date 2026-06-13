package com.carlosdaza.splitexpense.service;

import com.carlosdaza.splitexpense.domain.dto.SplitDto;
import com.carlosdaza.splitexpense.domain.entity.*;
import com.carlosdaza.splitexpense.exception.BusinessException;
import com.carlosdaza.splitexpense.exception.ResourceNotFoundException;
import com.carlosdaza.splitexpense.kafka.producer.ExpenseEventProducer;
import com.carlosdaza.splitexpense.repository.ExpenseRepository;
import com.carlosdaza.splitexpense.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final ExpenseEventProducer eventProducer;

    @Transactional
    public SplitDto.ExpenseResponse createExpense(UUID groupId, UUID payerId,
                                                   SplitDto.ExpenseRequest request) {
        Group group = groupService.findGroupOrThrow(groupId);
        User paidBy = findUserOrThrow(payerId);

        if (!group.getMembers().contains(paidBy)) {
            throw new BusinessException("Payer is not a member of the group.");
        }

        // Determine who to split among
        List<User> splitAmong;
        if (request.splitAmongUserIds() != null && !request.splitAmongUserIds().isEmpty()) {
            splitAmong = userRepository.findAllById(request.splitAmongUserIds());
            boolean allMembers = splitAmong.stream().allMatch(group.getMembers()::contains);
            if (!allMembers) throw new BusinessException("All split users must be group members.");
        } else {
            splitAmong = new ArrayList<>(group.getMembers());
        }

        BigDecimal perPerson = request.amount()
                .divide(BigDecimal.valueOf(splitAmong.size()), 2, RoundingMode.HALF_UP);

        Expense expense = Expense.builder()
                .group(group)
                .paidBy(paidBy)
                .description(request.description())
                .amount(request.amount())
                .currency(request.currency())
                .expenseDate(request.expenseDate())
                .build();

        List<ExpenseSplit> splits = splitAmong.stream()
                .map(u -> ExpenseSplit.builder()
                        .expense(expense)
                        .user(u)
                        .amountOwed(perPerson)
                        .settled(u.getId().equals(payerId)) // payer's own split is auto-settled
                        .settledAt(u.getId().equals(payerId) ? LocalDateTime.now() : null)
                        .build())
                .toList();

        expense.getSplits().addAll(splits);
        Expense saved = expenseRepository.save(expense);
        log.info("Expense created: id={} group={} amount={}", saved.getId(), groupId, request.amount());

        eventProducer.publishExpenseEvent(SplitDto.ExpenseEvent.builder()
                .expenseId(saved.getId()).groupId(groupId)
                .groupName(group.getName()).paidById(payerId)
                .paidByName(paidBy.getName()).amount(request.amount())
                .currency(request.currency()).description(request.description())
                .eventType("EXPENSE_CREATED").occurredAt(LocalDateTime.now())
                .build());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SplitDto.ExpenseResponse> getGroupExpenses(UUID groupId) {
        groupService.findGroupOrThrow(groupId);
        return expenseRepository.findByGroupIdOrderByExpenseDateDesc(groupId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SplitDto.GroupBalanceResponse getGroupBalance(UUID groupId) {
        Group group = groupService.findGroupOrThrow(groupId);
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByExpenseDateDesc(groupId);

        // Calculate net balance per user
        Map<UUID, BigDecimal> paid = new HashMap<>();
        Map<UUID, BigDecimal> owed = new HashMap<>();

        for (User m : group.getMembers()) {
            paid.put(m.getId(), BigDecimal.ZERO);
            owed.put(m.getId(), BigDecimal.ZERO);
        }

        for (Expense e : expenses) {
            paid.merge(e.getPaidBy().getId(), e.getAmount(), BigDecimal::add);
            for (ExpenseSplit s : e.getSplits()) {
                if (!s.isSettled()) {
                    owed.merge(s.getUser().getId(), s.getAmountOwed(), BigDecimal::add);
                }
            }
        }

        List<SplitDto.BalanceResponse> balances = group.getMembers().stream().map(u -> {
            BigDecimal totalPaid = paid.getOrDefault(u.getId(), BigDecimal.ZERO);
            BigDecimal totalOwed = owed.getOrDefault(u.getId(), BigDecimal.ZERO);
            return SplitDto.BalanceResponse.builder()
                    .userId(u.getId()).userName(u.getName())
                    .totalPaid(totalPaid).totalOwed(totalOwed)
                    .netBalance(totalPaid.subtract(totalOwed))
                    .build();
        }).toList();

        // Build direct debts
        List<SplitDto.DebtResponse> debts = new ArrayList<>();
        for (Expense e : expenses) {
            for (ExpenseSplit s : e.getSplits()) {
                if (!s.isSettled() && !s.getUser().getId().equals(e.getPaidBy().getId())) {
                    debts.add(SplitDto.DebtResponse.builder()
                            .fromUserId(s.getUser().getId())
                            .fromUserName(s.getUser().getName())
                            .toUserId(e.getPaidBy().getId())
                            .toUserName(e.getPaidBy().getName())
                            .amount(s.getAmountOwed())
                            .currency(e.getCurrency())
                            .build());
                }
            }
        }

        return SplitDto.GroupBalanceResponse.builder()
                .groupId(groupId).groupName(group.getName())
                .memberBalances(balances).debts(debts)
                .build();
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }

    private SplitDto.ExpenseResponse toResponse(Expense e) {
        List<SplitDto.SplitResponse> splits = e.getSplits().stream()
                .map(s -> SplitDto.SplitResponse.builder()
                        .userId(s.getUser().getId()).userName(s.getUser().getName())
                        .amountOwed(s.getAmountOwed()).settled(s.isSettled())
                        .settledAt(s.getSettledAt()).build())
                .toList();

        return SplitDto.ExpenseResponse.builder()
                .id(e.getId()).groupId(e.getGroup().getId()).groupName(e.getGroup().getName())
                .paidById(e.getPaidBy().getId()).paidByName(e.getPaidBy().getName())
                .description(e.getDescription()).amount(e.getAmount()).currency(e.getCurrency())
                .expenseDate(e.getExpenseDate()).splits(splits).createdAt(e.getCreatedAt())
                .build();
    }
}
