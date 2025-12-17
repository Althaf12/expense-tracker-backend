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
    List<Income> findByUsernameAndReceivedDateBetween(String userId, LocalDate start, LocalDate end);
    Page<Income> findByUsernameAndReceivedDateBetween(String userId, LocalDate start, LocalDate end, Pageable pageable);
    List<Income> findByUsername(String username);
    Page<Income> findByUsername(String username, Pageable pageable);
    void deleteByUsername(String username);
}
