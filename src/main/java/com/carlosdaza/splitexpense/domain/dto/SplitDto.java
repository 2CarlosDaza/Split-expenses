package com.carlosdaza.splitexpense.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SplitDto {

    // ── Auth ─────────────────────────────────────────────────────────────────

    @Builder
    public record RegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password
    ) {}

    @Builder
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    @Builder
    public record AuthResponse(
            String accessToken,
            String tokenType,
            UUID userId,
            String name,
            String email
    ) {}

    // ── User ─────────────────────────────────────────────────────────────────

    @Builder
    public record UserResponse(
            UUID id,
            String name,
            String email,
            LocalDateTime createdAt
    ) {}

    // ── Group ────────────────────────────────────────────────────────────────

    @Builder
    public record GroupRequest(
            @NotBlank @Size(max = 100) String name,
            String description
    ) {}

    @Builder
    public record GroupResponse(
            UUID id,
            String name,
            String description,
            UUID createdBy,
            String createdByName,
            List<UserResponse> members,
            LocalDateTime createdAt
    ) {}

    // ── Expense ───────────────────────────────────────────────────────────────

    @Builder
    public record ExpenseRequest(
            @NotBlank String description,

            @NotNull @DecimalMin("0.01") BigDecimal amount,

            @NotBlank @Size(min = 3, max = 3) String currency,

            @NotNull LocalDate expenseDate,

            // Optional: if null, split equally among all group members
            List<UUID> splitAmongUserIds
    ) {}

    @Builder
    public record ExpenseResponse(
            UUID id,
            UUID groupId,
            String groupName,
            UUID paidById,
            String paidByName,
            String description,
            BigDecimal amount,
            String currency,
            LocalDate expenseDate,
            List<SplitResponse> splits,
            LocalDateTime createdAt
    ) {}

    @Builder
    public record SplitResponse(
            UUID userId,
            String userName,
            BigDecimal amountOwed,
            boolean settled,
            LocalDateTime settledAt
    ) {}

    // ── Balance ───────────────────────────────────────────────────────────────

    @Builder
    public record BalanceResponse(
            UUID userId,
            String userName,
            BigDecimal totalPaid,
            BigDecimal totalOwed,
            BigDecimal netBalance   // positive = others owe you, negative = you owe others
    ) {}

    @Builder
    public record GroupBalanceResponse(
            UUID groupId,
            String groupName,
            List<BalanceResponse> memberBalances,
            List<DebtResponse> debts
    ) {}

    @Builder
    public record DebtResponse(
            UUID fromUserId,
            String fromUserName,
            UUID toUserId,
            String toUserName,
            BigDecimal amount,
            String currency
    ) {}

    // ── Settlement ────────────────────────────────────────────────────────────

    @Builder
    public record SettlementRequest(
            @NotNull UUID payeeId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Size(min = 3, max = 3) String currency
    ) {}

    @Builder
    public record SettlementResponse(
            UUID id,
            UUID groupId,
            UUID payerId,
            String payerName,
            UUID payeeId,
            String payeeName,
            BigDecimal amount,
            String currency,
            LocalDateTime settledAt
    ) {}

    // ── Kafka Events ──────────────────────────────────────────────────────────

    @Builder
    public record ExpenseEvent(
            UUID expenseId,
            UUID groupId,
            String groupName,
            UUID paidById,
            String paidByName,
            BigDecimal amount,
            String currency,
            String description,
            String eventType,
            LocalDateTime occurredAt
    ) {}
}
