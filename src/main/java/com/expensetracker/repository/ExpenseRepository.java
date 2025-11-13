package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {
    List<Expense> findByUsername(String username);
    List<Expense> findByUsernameAndExpenseDateBetween(String username, LocalDate start, LocalDate end);
    void deleteByUsername(String username);
}
