package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {
    List<Expense> findByUserId(String userId);
    Page<Expense> findByUserId(String userId, Pageable pageable);
    List<Expense> findByUserIdAndExpenseDateBetween(String userId, LocalDate start, LocalDate end);
    Page<Expense> findByUserIdAndExpenseDateBetween(String userId, LocalDate start, LocalDate end, Pageable pageable);
    void deleteByUserId(String userId);
    boolean existsByUserExpenseCategoryId(Integer userExpenseCategoryId);
    @Query("SELECT DISTINCT e.userExpenseCategoryId FROM Expense e WHERE e.userId = :userId AND e.userExpenseCategoryId IS NOT NULL")
    List<Integer> findDistinctUserExpenseCategoryIdByUserId(@Param("userId") String userId);
}
