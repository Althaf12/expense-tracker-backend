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
    List<Expense> findByUsername(String username);
    Page<Expense> findByUsername(String username, Pageable pageable);
    List<Expense> findByUsernameAndExpenseDateBetween(String username, LocalDate start, LocalDate end);
    Page<Expense> findByUsernameAndExpenseDateBetween(String username, LocalDate start, LocalDate end, Pageable pageable);
    void deleteByUsername(String username);
    boolean existsByUserExpenseCategoryId(Integer userExpenseCategoryId);
    @Query("SELECT DISTINCT e.userExpenseCategoryId FROM Expense e WHERE e.username = :username AND e.userExpenseCategoryId IS NOT NULL")
    List<Integer> findDistinctUserExpenseCategoryIdByUsername(@Param("username") String username);
}
