package com.expensetracker.repository;

import com.expensetracker.model.UserExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserExpenseCategoryRepository extends JpaRepository<UserExpenseCategory, Integer> {
    List<UserExpenseCategory> findByUsernameOrderByUserExpenseCategoryName(String username);
    Optional<UserExpenseCategory> findByUserExpenseCategoryIdAndUsername(Integer id, String username);
    @Modifying
    @Transactional
    @Query("delete from UserExpenseCategory u where u.username = :username")
    void deleteByUsername(@Param("username") String username);
    int countByUsername(String username);
}
