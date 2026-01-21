package com.expensetracker.admin.service;
import com.expensetracker.admin.model.PlannedExpenses;
import com.expensetracker.admin.repository.PlannedExpensesRepository;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.exception.UserNotFoundException;
import com.expensetracker.model.UserExpenses;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.repository.UserExpenseCategoryRepository;
import com.expensetracker.repository.UserExpensesRepository;
import com.expensetracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
@Service("adminPlannedExpensesService")
public class PlannedExpensesService {
    private static final Logger logger = LoggerFactory.getLogger(PlannedExpensesService.class);
    private final PlannedExpensesRepository plannedExpensesRepository;
    private final UserExpenseCategoryRepository userExpenseCategoryRepository;
    private final UserExpensesRepository userExpensesRepository;
    private final UserRepository userRepository;
    @Autowired
    public PlannedExpensesService(@Qualifier("adminPlannedExpensesRepository") PlannedExpensesRepository plannedExpensesRepository,
                                  UserExpenseCategoryRepository userExpenseCategoryRepository,
                                  UserExpensesRepository userExpensesRepository,
                                  UserRepository userRepository) {
        this.plannedExpensesRepository = plannedExpensesRepository;
        this.userExpenseCategoryRepository = userExpenseCategoryRepository;
        this.userExpensesRepository = userExpensesRepository;
        this.userRepository = userRepository;
    }
    public List<PlannedExpenses> findAll() { return plannedExpensesRepository.findAllByOrderByExpenseName(); }
    public Optional<PlannedExpenses> findById(Integer id) { return plannedExpensesRepository.findById(id); }
    @Transactional
    public PlannedExpenses save(PlannedExpenses p) {
        if (p.getLastUpdateTmstp() == null) p.setLastUpdateTmstp(LocalDateTime.now());
        return plannedExpensesRepository.save(p);
    }
    @Transactional
    public void deleteById(Integer id) {
        if (!plannedExpensesRepository.existsById(id)) throw new ResourceNotFoundException("PlannedExpenses", "id", id);
        plannedExpensesRepository.deleteById(id);
    }
    @Transactional
    @CacheEvict(cacheNames = "userExpenses", key = "#userId")
    public int copyPlannedToUser(String userId) {
        if (!userRepository.existsById(userId)) throw new UserNotFoundException(userId);
        int inserted = 0;
        List<PlannedExpenses> planned = plannedExpensesRepository.findAllByOrderByExpenseName();
        if (planned == null || planned.isEmpty()) return inserted;
        List<UserExpenseCategory> userCats = userExpenseCategoryRepository.findByUserIdOrderByUserExpenseCategoryName(userId);
        if (userCats == null || userCats.isEmpty()) return inserted;
        Map<String, Integer> nameToId = userCats.stream()
                .filter(c -> c.getUserExpenseCategoryName() != null)
                .collect(Collectors.toMap(c -> c.getUserExpenseCategoryName().trim().toLowerCase(),
                        UserExpenseCategory::getUserExpenseCategoryId, (existing, replacement) -> existing));
        for (PlannedExpenses pe : planned) {
            String expenseName = pe.getExpenseName();
            String expenseCategory = pe.getExpenseCategory();
            if (expenseName == null || expenseCategory == null) continue;
            String catNorm = expenseCategory.trim().toLowerCase();
            Integer catId = nameToId.get(catNorm);
            if (catId == null) continue;
            boolean exists = userExpensesRepository.existsByUserIdAndUserExpenseNameIgnoreCaseAndUserExpenseCategoryId(userId, expenseName.trim(), catId);
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
        logger.info("Copied {} planned expenses for userId: {}", inserted, userId);
        return inserted;
    }
}
