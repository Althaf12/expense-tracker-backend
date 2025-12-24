package com.expensetracker.repository;

import com.expensetracker.model.Income;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Integer> {
    List<Income> findByUserIdAndReceivedDateBetween(String userId, LocalDate start, LocalDate end);
    Page<Income> findByUserIdAndReceivedDateBetween(String userId, LocalDate start, LocalDate end, Pageable pageable);
    List<Income> findByUserId(String userId);
    Page<Income> findByUserId(String userId, Pageable pageable);
    void deleteByUserId(String userId);
}
