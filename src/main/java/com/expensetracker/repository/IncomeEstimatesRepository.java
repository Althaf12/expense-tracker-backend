package com.expensetracker.repository;

import com.expensetracker.model.IncomeEstimates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeEstimatesRepository extends JpaRepository<IncomeEstimates, Integer> {

    List<IncomeEstimates> findByUserIdOrderByYearDescMonthAsc(String userId);

    List<IncomeEstimates> findByUserIdAndMonthAndYearOrderByReceivedDateAsc(String userId, String month, Integer year);

    Optional<IncomeEstimates> findByIncomeEstimatesIdAndUserId(Integer id, String userId);

    int countByUserId(String userId);
}

