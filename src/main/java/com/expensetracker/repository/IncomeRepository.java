package com.expensetracker.repository;

import com.expensetracker.model.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Integer> {
    List<Income> findByUserId(String userId);
    List<Income> findByUserIdAndReceivedDateBetween(String userId, LocalDate start, LocalDate end);
}

