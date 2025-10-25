package com.expensetracker.service;

import com.expensetracker.model.Income;
import com.expensetracker.repository.IncomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;

    @Autowired
    public IncomeService(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    public Income addIncome(Income income) {
        return incomeRepository.save(income);
    }

    public List<Income> getByUser(String userId) {
        return incomeRepository.findByUserId(userId);
    }

    public List<Income> getByUserAndDateRange(String userId, LocalDate start, LocalDate end) {
        return incomeRepository.findByUserIdAndReceivedDateBetween(userId, start, end);
    }

    public void deleteIncome(Integer incomeId) {
        incomeRepository.deleteById(incomeId);
    }
}

