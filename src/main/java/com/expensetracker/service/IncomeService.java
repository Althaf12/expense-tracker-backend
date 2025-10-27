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

    public Optional<Income> findById(Integer id) {
        return incomeRepository.findById(id);
    }

    public Income updateIncome(Integer incomeId, String userId, Income updated) {
        Optional<Income> opt = incomeRepository.findById(incomeId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("income not found");
        }
        Income existing = opt.get();
        if (userId != null && !userId.equals(existing.getUserId())) {
            throw new IllegalArgumentException("userId mismatch");
        }
        if (updated.getSource() != null) existing.setSource(updated.getSource());
        if (updated.getAmount() != null) existing.setAmount(updated.getAmount());
        if (updated.getReceivedDate() != null) existing.setReceivedDate(updated.getReceivedDate());
        return incomeRepository.save(existing);
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
