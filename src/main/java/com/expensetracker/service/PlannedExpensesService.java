package com.expensetracker.service;

import com.expensetracker.model.PlannedExpenses;
import com.expensetracker.model.UserExpenses;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.repository.PlannedExpensesRepository;
import com.expensetracker.repository.UserExpenseCategoryRepository;
import com.expensetracker.repository.UserExpensesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlannedExpensesService {

    private final PlannedExpensesRepository plannedExpensesRepository;
    private final UserExpenseCategoryRepository userExpenseCategoryRepository;
    private final UserExpensesRepository userExpensesRepository;

    @Autowired
    public PlannedExpensesService(PlannedExpensesRepository plannedExpensesRepository,
                                  UserExpenseCategoryRepository userExpenseCategoryRepository,
                                  UserExpensesRepository userExpensesRepository) {
        this.plannedExpensesRepository = plannedExpensesRepository;
        this.userExpenseCategoryRepository = userExpenseCategoryRepository;
        this.userExpensesRepository = userExpensesRepository;
    }

    public List<PlannedExpenses> findAll() {
        return plannedExpensesRepository.findAllByOrderByExpenseName();
    }

    public Optional<PlannedExpenses> findById(Integer id) {
        return plannedExpensesRepository.findById(id);
    }

    @Transactional
    public PlannedExpenses save(PlannedExpenses p) {
        if (p.getLastUpdateTmstp() == null) p.setLastUpdateTmstp(LocalDateTime.now());
        return plannedExpensesRepository.save(p);
    }

    @Transactional
    public void deleteById(Integer id) {
        plannedExpensesRepository.deleteById(id);
    }

    /**
     * Copy planned expenses into user's user_expenses table using the newly created user categories.
     */
    @Transactional
    @CacheEvict(cacheNames = "userExpenses", key = "#userId")
    public int copyPlannedToUser(String userId) {
        int inserted = 0;
        List<PlannedExpenses> planned = plannedExpensesRepository.findAllByOrderByExpenseName();
        if (planned == null || planned.isEmpty()) return inserted;

        List<UserExpenseCategory> userCats = userExpenseCategoryRepository.findByUserIdOrderByUserExpenseCategoryName(userId);
        if (userCats == null || userCats.isEmpty()) return inserted;

        // Map category name to id for quick lookup
        Map<String, Integer> nameToId = userCats.stream()
                .filter(c -> c.getUserExpenseCategoryName() != null)
                .collect(Collectors.toMap(c -> c.getUserExpenseCategoryName().trim().toLowerCase(), UserExpenseCategory::getUserExpenseCategoryId));

        for (PlannedExpenses pe : planned) {
            String expenseName = pe.getExpenseName();
            String expenseCategory = pe.getExpenseCategory();
            if (expenseName == null || expenseCategory == null) continue;

            String catNorm = expenseCategory.trim().toLowerCase();
            Integer catId = nameToId.get(catNorm);
            if (catId == null) continue;

            // Check if user_expenses already has an entry with same userId, expense name and category id
            boolean exists = userExpensesRepository.existsByUserIdAndUserExpenseNameIgnoreCaseAndUserExpenseCategoryId(
                    userId, expenseName.trim(), catId);
            if (exists) continue;

            UserExpenses ue = new UserExpenses();
            ue.setUserId(userId);
            ue.setUserExpenseName(expenseName.trim());
            ue.setUserExpenseCategoryId(catId);
            ue.setAmount(pe.getExpenseAmount());
            ue.setPaid("N");
            ue.setStatus("A");
            ue.setLastUpdateTmstp(LocalDateTime.now());
            userExpensesRepository.save(ue);
            inserted++;
        }
        return inserted;
    }
}
