package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {
    List<Expense> findByUserId(String userId);
    List<Expense> findByUserIdAndExpenseDateBetween(String userId, LocalDate start, LocalDate end);
    void deleteByUserIdAndExpensesId(String userId, Integer expensesId);
}
