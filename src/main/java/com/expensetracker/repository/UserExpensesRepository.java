package com.expensetracker.repository;

import com.expensetracker.model.UserExpenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    // find a user expense by name and category (used for sync from estimates)
    Optional<UserExpenses> findByUserIdAndUserExpenseNameIgnoreCaseAndUserExpenseCategoryId(String userId, String userExpenseName, Integer userExpenseCategoryId);

    // find all active user_expenses for a user in a specific set of categories (used for tombstone pass)
    List<UserExpenses> findByUserIdAndStatusAndUserExpenseCategoryIdIn(String userId, String status, Collection<Integer> categoryIds);

    // find all active user_expenses for a user in a single category (used for CC tombstone pass)
    List<UserExpenses> findByUserIdAndStatusAndUserExpenseCategoryId(String userId, String status, Integer categoryId);
}
