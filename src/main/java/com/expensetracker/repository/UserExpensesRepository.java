package com.expensetracker.repository;

import com.expensetracker.model.UserExpenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserExpensesRepository extends JpaRepository<UserExpenses, Integer> {
    List<UserExpenses> findByUserIdOrderByUserExpenseName(String userId);
    List<UserExpenses> findByUserIdAndStatusOrderByUserExpenseName(String userId, String status);
    Optional<UserExpenses> findByUserExpensesIdAndUserId(Integer id, String userId);
    int countByUserId(String userId);

    // check if any user_expense references given user expense category id
    boolean existsByUserExpenseCategoryId(Integer userExpenseCategoryId);

    // check if a particular user expense with same name and category exists for the user
    boolean existsByUserIdAndUserExpenseNameIgnoreCaseAndUserExpenseCategoryId(String userId, String userExpenseName, Integer userExpenseCategoryId);
}
