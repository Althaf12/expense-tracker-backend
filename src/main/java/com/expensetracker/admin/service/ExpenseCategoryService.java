package com.expensetracker.admin.service;
import com.expensetracker.admin.model.ExpenseCategory;
import com.expensetracker.admin.repository.ExpenseCategoryRepository;
import com.expensetracker.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service("adminExpenseCategoryService")
public class ExpenseCategoryService {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseCategoryService.class);
    private final ExpenseCategoryRepository expenseCategoryRepository;
    @Autowired
    public ExpenseCategoryService(@Qualifier("adminExpenseCategoryRepository") ExpenseCategoryRepository expenseCategoryRepository) {
        this.expenseCategoryRepository = expenseCategoryRepository;
    }
    public ExpenseCategory addOrUpdate(ExpenseCategory c) {
        if (c.getLastUpdateTmstp() == null) c.setLastUpdateTmstp(LocalDateTime.now());
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
        if (!expenseCategoryRepository.existsById(id)) throw new ResourceNotFoundException("ExpenseCategory", "id", id);
        expenseCategoryRepository.deleteById(id);
    }
    public ExpenseCategory updateById(Integer id, String newName) {
        Optional<ExpenseCategory> opt = expenseCategoryRepository.findById(id);
        if (opt.isEmpty()) throw new ResourceNotFoundException("ExpenseCategory", "id", id);
        ExpenseCategory c = opt.get();
        if (newName != null && !newName.isBlank()) c.setExpenseCategoryName(newName);
        c.setLastUpdateTmstp(LocalDateTime.now());
        return expenseCategoryRepository.save(c);
    }
    public ExpenseCategory updateByName(String name, String newName) {
        Optional<ExpenseCategory> opt = expenseCategoryRepository.findByExpenseCategoryName(name);
        if (opt.isEmpty()) throw new ResourceNotFoundException("ExpenseCategory", "name", name);
        ExpenseCategory c = opt.get();
        if (newName != null && !newName.isBlank()) c.setExpenseCategoryName(newName);
        c.setLastUpdateTmstp(LocalDateTime.now());
        return expenseCategoryRepository.save(c);
    }
}
