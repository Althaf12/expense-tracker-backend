package com.expensetracker.repository;

import com.expensetracker.model.UserExpensesEstimates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserExpensesEstimatesRepository extends JpaRepository<UserExpensesEstimates, Integer> {

    List<UserExpensesEstimates> findByUserIdOrderByUserExpenseName(String userId);

    List<UserExpensesEstimates> findByUserIdAndStatusOrderByUserExpenseName(String userId, String status);

    Optional<UserExpensesEstimates> findByUserExpensesEstimatesIdAndUserId(Integer id, String userId);

    int countByUserId(String userId);

    boolean existsByUserIdAndUserExpenseNameIgnoreCaseAndUserExpenseCategoryId(
            String userId, String userExpenseName, Integer userExpenseCategoryId);

    Optional<UserExpensesEstimates> findByUserIdAndUserExpenseNameIgnoreCaseAndUserExpenseCategoryId(
            String userId, String userExpenseName, Integer userExpenseCategoryId);

    @Query("SELECT DISTINCT e.userId FROM UserExpensesEstimates e")
    List<String> findDistinctUserIds();
}

