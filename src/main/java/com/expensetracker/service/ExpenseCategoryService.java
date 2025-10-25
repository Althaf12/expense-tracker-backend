package com.expensetracker.service;

import com.expensetracker.model.ExpenseCategory;
import com.expensetracker.repository.ExpenseCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    @Autowired
    public ExpenseCategoryService(ExpenseCategoryRepository expenseCategoryRepository) {
        this.expenseCategoryRepository = expenseCategoryRepository;
    }

    public ExpenseCategory addOrUpdate(ExpenseCategory c) {
        if (c.getLastUpdateTmstp() == null) {
            c.setLastUpdateTmstp(LocalDateTime.now());
        }
        return expenseCategoryRepository.save(c);
    }

    public Optional<ExpenseCategory> findById(Integer id) {
        return expenseCategoryRepository.findById(id);
    }

    public List<ExpenseCategory> findAll() {
        return expenseCategoryRepository.findAll();
    }

    public void deleteById(Integer id) {
        expenseCategoryRepository.deleteById(id);
    }
}

