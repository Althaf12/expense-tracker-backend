package com.expensetracker.repository;

import com.expensetracker.model.UserExpenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserExpensesRepository extends JpaRepository<UserExpenses, Integer> {
    List<UserExpenses> findByUsernameOrderByUserExpenseName(String username);
    List<UserExpenses> findByUsernameAndStatusOrderByUserExpenseName(String username, String status);
    Optional<UserExpenses> findByUserExpensesIdAndUsername(Integer id, String username);
    int countByUsername(String username);

    // check if any user_expense references given user expense category id
    boolean existsByUserExpenseCategoryId(Integer userExpenseCategoryId);

    // check if a particular user expense with same name and category exists for the user
    boolean existsByUsernameAndUserExpenseNameIgnoreCaseAndUserExpenseCategoryId(String username, String userExpenseName, Integer userExpenseCategoryId);
}
