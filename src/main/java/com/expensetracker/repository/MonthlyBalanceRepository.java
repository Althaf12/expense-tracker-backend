package com.expensetracker.repository;

import com.expensetracker.model.MonthlyBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyBalanceRepository extends JpaRepository<MonthlyBalance, Long> {

    @Query("SELECT mb FROM MonthlyBalance mb WHERE mb.username = :username ORDER BY mb.year DESC, mb.month DESC")
    Optional<MonthlyBalance> findTopByUsernameOrderByYearDescMonthDesc(@Param("username") String username);

    Optional<MonthlyBalance> findByUsernameAndYearAndMonth(String username, Integer year, Integer month);

}

