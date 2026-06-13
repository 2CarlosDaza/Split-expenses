package com.carlosdaza.splitexpense.repository;

import com.carlosdaza.splitexpense.domain.entity.Expense;
import com.carlosdaza.splitexpense.domain.entity.ExpenseSplit;
import com.carlosdaza.splitexpense.domain.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByGroupIdOrderByExpenseDateDesc(UUID groupId);
}

@Repository
interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, UUID> {

    List<ExpenseSplit> findByExpenseId(UUID expenseId);

    @Query("SELECT s FROM ExpenseSplit s WHERE s.user.id = :userId AND s.settled = false")
    List<ExpenseSplit> findUnsettledByUser(@Param("userId") UUID userId);

    @Query("""
        SELECT COALESCE(SUM(s.amountOwed), 0)
        FROM ExpenseSplit s
        WHERE s.user.id = :userId
          AND s.expense.group.id = :groupId
          AND s.settled = false
        """)
    BigDecimal sumUnsettledByUserAndGroup(
            @Param("userId") UUID userId,
            @Param("groupId") UUID groupId);
}

@Repository
interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findByGroupIdOrderBySettledAtDesc(UUID groupId);
}
