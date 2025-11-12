package com.expensetracker.repository;

import com.expensetracker.model.UserExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserExpenseCategoryRepository extends JpaRepository<UserExpenseCategory, Integer> {
    List<UserExpenseCategory> findByUsernameOrderByUserExpenseCategoryName(String username);
    Optional<UserExpenseCategory> findByUserExpenseCategoryIdAndUsername(Integer id, String username);
    void deleteByUsername(String username);
    int countByUsername(String username);
}
