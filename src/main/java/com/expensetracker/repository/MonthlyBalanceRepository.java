package com.expensetracker.repository;

import com.expensetracker.model.MonthlyBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyBalanceRepository extends JpaRepository<MonthlyBalance, Long> {

    @Query("SELECT mb FROM MonthlyBalance mb WHERE mb.userId = :userId ORDER BY mb.year DESC, mb.month DESC")
    Optional<MonthlyBalance> findTopByUserIdOrderByYearDescMonthDesc(@Param("userId") String userId);

    Optional<MonthlyBalance> findByUserIdAndYearAndMonth(String userId, Integer year, Integer month);
}
