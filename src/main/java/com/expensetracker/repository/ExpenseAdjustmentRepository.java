package com.expensetracker.repository;

import com.expensetracker.model.ExpenseAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseAdjustmentRepository extends JpaRepository<ExpenseAdjustment, Integer> {

    List<ExpenseAdjustment> findByUserId(String userId);

    Page<ExpenseAdjustment> findByUserId(String userId, Pageable pageable);

    List<ExpenseAdjustment> findByExpensesId(Integer expensesId);

    List<ExpenseAdjustment> findByExpensesIdAndStatus(Integer expensesId, String status);

    List<ExpenseAdjustment> findByUserIdAndStatus(String userId, String status);

    List<ExpenseAdjustment> findByUserIdAndAdjustmentDateBetween(String userId, LocalDate start, LocalDate end);

    Page<ExpenseAdjustment> findByUserIdAndAdjustmentDateBetween(String userId, LocalDate start, LocalDate end, Pageable pageable);

    /**
     * Get total completed adjustment amount for a specific expense.
     * Only considers adjustments with status 'COMPLETED'.
     */
    @Query("SELECT COALESCE(SUM(ea.adjustmentAmount), 0) FROM ExpenseAdjustment ea " +
           "WHERE ea.expensesId = :expensesId AND ea.status = 'COMPLETED'")
    BigDecimal getTotalCompletedAdjustmentForExpense(@Param("expensesId") Integer expensesId);

    /**
     * Get total completed adjustments for a user within a date range.
     */
    @Query("SELECT COALESCE(SUM(ea.adjustmentAmount), 0) FROM ExpenseAdjustment ea " +
           "WHERE ea.userId = :userId AND ea.status = 'COMPLETED' " +
           "AND ea.adjustmentDate BETWEEN :start AND :end")
    BigDecimal getTotalCompletedAdjustmentsForUserInRange(
            @Param("userId") String userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * Get all completed adjustments for expenses in a list of expense IDs.
     */
    @Query("SELECT ea FROM ExpenseAdjustment ea WHERE ea.expensesId IN :expenseIds AND ea.status = 'COMPLETED'")
    List<ExpenseAdjustment> findCompletedAdjustmentsForExpenses(@Param("expenseIds") List<Integer> expenseIds);

    void deleteByUserId(String userId);

    void deleteByExpensesId(Integer expensesId);

    boolean existsByExpensesId(Integer expensesId);
}
