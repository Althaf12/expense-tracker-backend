package com.expensetracker.repository;

import com.expensetracker.model.UserExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserExpenseRepository extends JpaRepository<UserExpense, Integer> {
    List<UserExpense> findByUsernameOrderByUserExpenseName(String username);
    List<UserExpense> findByUsernameAndStatusOrderByUserExpenseName(String username, String status);
    Optional<UserExpense> findByUserExpensesIdAndUsername(Integer id, String username);
    int countByUsername(String username);
    
    @Modifying
    @Transactional
    @Query("delete from UserExpense u where u.username = :username")
    void deleteByUsername(@Param("username") String username);
}
