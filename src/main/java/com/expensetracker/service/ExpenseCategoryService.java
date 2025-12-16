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

    public Optional<ExpenseCategory> findByName(String name) {
        return expenseCategoryRepository.findByExpenseCategoryName(name);
    }

    public List<ExpenseCategory> findAll() {
        return expenseCategoryRepository.findAll();
    }

    public void deleteById(Integer id) {
        expenseCategoryRepository.deleteById(id);
    }

    public ExpenseCategory updateById(Integer id, String newName) {
        Optional<ExpenseCategory> opt = expenseCategoryRepository.findById(id);
        if (opt.isEmpty()) throw new IllegalArgumentException("category not found");
        ExpenseCategory c = opt.get();
        if (newName != null && !newName.isBlank()) c.setExpenseCategoryName(newName);
        c.setLastUpdateTmstp(LocalDateTime.now());
        return expenseCategoryRepository.save(c);
    }

    public ExpenseCategory updateByName(String name, String newName) {
        Optional<ExpenseCategory> opt = expenseCategoryRepository.findByExpenseCategoryName(name);
        if (opt.isEmpty()) throw new IllegalArgumentException("category not found");
        ExpenseCategory c = opt.get();
        if (newName != null && !newName.isBlank()) c.setExpenseCategoryName(newName);
        c.setLastUpdateTmstp(LocalDateTime.now());
        return expenseCategoryRepository.save(c);
    }
}
