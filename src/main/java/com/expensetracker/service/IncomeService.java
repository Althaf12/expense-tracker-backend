package com.expensetracker.service;

import com.expensetracker.model.Income;
import com.expensetracker.repository.IncomeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@CacheConfig(cacheNames = "incomes")
@Service
public class IncomeService {

    private static final Logger logger = LoggerFactory.getLogger(IncomeService.class);

    private final IncomeRepository incomeRepository;
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 20, 50, 100);

    @Autowired
    public IncomeService(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    @CacheEvict(allEntries = true)
    public Income addIncome(Income income) {
        logger.info("Adding income for userId: {}", income.getUserId());
        return incomeRepository.save(income);
    }

    @CacheEvict(allEntries = true)
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
        logger.info("Updated income {} for userId: {}", incomeId, userId);
        return incomeRepository.save(existing);
    }

    @Cacheable(key = "#userId + ':' + #start + ':' + #end")
    public List<Income> getByUserAndDateRange(String userId, LocalDate start, LocalDate end) {
        return incomeRepository.findByUserIdAndReceivedDateBetween(userId, start, end);
    }

    @Cacheable(key = "#userId + ':' + #start + ':' + #end + ':' + #page + ':' + #size")
    public Page<Income> getByUserAndDateRange(String userId, LocalDate start, LocalDate end, int page, int size) {
        if (!ALLOWED_PAGE_SIZES.contains(size)) throw new IllegalArgumentException("invalid page size");
        PageRequest pr = PageRequest.of(Math.max(0, page), size);
        Page<Income> p = incomeRepository.findByUserIdAndReceivedDateBetween(userId, start, end, pr);
        return new PageImpl<>(p.getContent(), pr, p.getTotalElements());
    }

    @CacheEvict(allEntries = true)
    public void deleteIncome(Integer incomeId) {
        logger.info("Deleting income: {}", incomeId);
        incomeRepository.deleteById(incomeId);
    }

    @CacheEvict(allEntries = true)
    public boolean deleteIncome(String userId, Integer incomeId) {
        if (userId == null || incomeId == null) return false;
        Optional<Income> opt = incomeRepository.findById(incomeId);
        if (opt.isEmpty()) return false;
        Income inc = opt.get();
        if (inc.getUserId() == null || !inc.getUserId().equals(userId)) return false;
        incomeRepository.deleteById(incomeId);
        logger.info("Deleted income {} for userId: {}", incomeId, userId);
        return true;
    }

    @CacheEvict(allEntries = true)
    public void deleteAllByUserId(String userId) {
        if (userId == null) return;
        logger.info("Deleting all incomes for userId: {}", userId);
        incomeRepository.deleteByUserId(userId);
    }
}
