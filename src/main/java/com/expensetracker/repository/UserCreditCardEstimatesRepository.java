package com.expensetracker.repository;

import com.expensetracker.model.UserCreditCardEstimates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCreditCardEstimatesRepository extends JpaRepository<UserCreditCardEstimates, Integer> {

    List<UserCreditCardEstimates> findByUserIdOrderByCardName(String userId);

    Optional<UserCreditCardEstimates> findByUserCreditCardEstimatesIdAndUserId(Integer id, String userId);

    int countByUserId(String userId);

    /** Returns all distinct userIds that have at least one credit card estimate. */
    @Query("SELECT DISTINCT c.userId FROM UserCreditCardEstimates c")
    List<String> findDistinctUserIds();
}

