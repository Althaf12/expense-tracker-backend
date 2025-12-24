package com.expensetracker.repository;

import com.expensetracker.model.UserExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserExpenseCategoryRepository extends JpaRepository<UserExpenseCategory, Integer> {
    List<UserExpenseCategory> findByUserIdOrderByUserExpenseCategoryName(String userId);
    Optional<UserExpenseCategory> findByUserExpenseCategoryIdAndUserId(Integer id, String userId);
    @Modifying
    @Transactional
    @Query("DELETE FROM UserExpenseCategory u WHERE u.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
    int countByUserId(String userId);
    List<UserExpenseCategory> findByUserIdAndStatusOrderByUserExpenseCategoryName(String userId, String status);
    Optional<UserExpenseCategory> findByUserIdAndUserExpenseCategoryName(String userId, String userExpenseCategoryName);
}
