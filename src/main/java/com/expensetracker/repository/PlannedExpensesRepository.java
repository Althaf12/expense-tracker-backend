package com.expensetracker.repository;

import com.expensetracker.model.PlannedExpenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlannedExpensesRepository extends JpaRepository<PlannedExpenses, Integer> {
    List<PlannedExpenses> findAllByOrderByExpenseName();
}

