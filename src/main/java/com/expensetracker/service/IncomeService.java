package com.expensetracker.service;

import com.expensetracker.model.Income;
import com.expensetracker.repository.IncomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@CacheConfig(cacheNames = "incomes")
@Service
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10,20,50,100);

    @Autowired
    public IncomeService(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    @CacheEvict(allEntries = true)
    public Income addIncome(Income income) {
        return incomeRepository.save(income);
    }

    @CacheEvict(allEntries = true)
    public Income updateIncome(Integer incomeId, String username, Income updated) {
        Optional<Income> opt = incomeRepository.findById(incomeId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("income not found");
        }
        Income existing = opt.get();
        if (username != null && !username.equals(existing.getUsername())) {
            throw new IllegalArgumentException("userId mismatch");
        }
        if (updated.getSource() != null) existing.setSource(updated.getSource());
        if (updated.getAmount() != null) existing.setAmount(updated.getAmount());
        if (updated.getReceivedDate() != null) existing.setReceivedDate(updated.getReceivedDate());
        return incomeRepository.save(existing);
    }

    @Cacheable(key = "#username + ':' + #start + ':' + #end")
    public List<Income> getByUserAndDateRange(String username, LocalDate start, LocalDate end) {
        return incomeRepository.findByUsernameAndReceivedDateBetween(username, start, end);
    }

    @Cacheable(key = "#username + ':' + #start + ':' + #end + ':' + #page + ':' + #size")
    public Page<Income> getByUserAndDateRange(String username, LocalDate start, LocalDate end, int page, int size) {
        if (!ALLOWED_PAGE_SIZES.contains(size)) throw new IllegalArgumentException("invalid page size");
        PageRequest pr = PageRequest.of(Math.max(0, page), size);
        Page<Income> p = incomeRepository.findByUsernameAndReceivedDateBetween(username, start, end, pr);
        return new PageImpl<>(p.getContent(), pr, p.getTotalElements());
    }

    @CacheEvict(allEntries = true)
    public void deleteIncome(Integer incomeId) {
        incomeRepository.deleteById(incomeId);
    }

    // New safe delete: ensure the income record belongs to the given username before deletion
    @CacheEvict(allEntries = true)
    public boolean deleteIncome(String username, Integer incomeId) {
        if (username == null || incomeId == null) return false;
        Optional<Income> opt = incomeRepository.findById(incomeId);
        if (opt.isEmpty()) return false;
        Income inc = opt.get();
        if (inc.getUsername() == null || !inc.getUsername().equals(username)) return false;
        incomeRepository.deleteById(incomeId);
        return true;
    }

    // delete all incomes for a username using a single repository query
    @CacheEvict(allEntries = true)
    public void deleteAllByUsername(String username) {
        if (username == null) return;
        incomeRepository.deleteByUsername(username);
    }
}
