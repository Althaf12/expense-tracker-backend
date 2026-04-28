package com.expensetracker.service;

import com.expensetracker.dto.IncomeEstimatesRequest;
import com.expensetracker.dto.IncomeEstimatesResponse;
import com.expensetracker.exception.IncomeEstimatesNotFoundException;
import com.expensetracker.model.Income;
import com.expensetracker.model.IncomeEstimates;
import com.expensetracker.repository.IncomeEstimatesRepository;
import com.expensetracker.repository.IncomeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncomeEstimatesService {

    private static final Logger logger = LoggerFactory.getLogger(IncomeEstimatesService.class);

    private final IncomeEstimatesRepository incomeEstimatesRepository;
    private final IncomeRepository incomeRepository;

    @Autowired
    public IncomeEstimatesService(IncomeEstimatesRepository incomeEstimatesRepository,
                                  IncomeRepository incomeRepository) {
        this.incomeEstimatesRepository = incomeEstimatesRepository;
        this.incomeRepository = incomeRepository;
    }

    // ─── Find All ─────────────────────────────────────────────────────────────

    public List<IncomeEstimatesResponse> findAll(String userId) {
        return incomeEstimatesRepository.findByUserIdOrderByYearDescMonthAsc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Find by Month & Year ─────────────────────────────────────────────────

    public List<IncomeEstimatesResponse> findByMonthAndYear(String userId, String month, Integer year) {
        return incomeEstimatesRepository
                .findByUserIdAndMonthAndYearOrderByReceivedDateAsc(userId, month, year)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Add ─────────────────────────────────────────────────────────────────

    @Transactional
    public IncomeEstimatesResponse add(String userId, IncomeEstimatesRequest request) {
        validateRequest(request, true);

        IncomeEstimates estimate = new IncomeEstimates();
        estimate.setUserId(userId);
        estimate.setSource(request.getSource() != null && !request.getSource().isBlank()
                ? request.getSource().trim() : "Salary");
        estimate.setAmount(request.getAmount());
        estimate.setReceivedDate(request.getReceivedDate());
        estimate.setMonth(request.getMonth().trim());
        estimate.setYear(request.getYear());
        estimate.setLastUpdateTmstp(LocalDateTime.now());

        IncomeEstimates saved = incomeEstimatesRepository.save(estimate);
        logger.info("Added income estimate id={} for userId={}", saved.getIncomeEstimatesId(), userId);
        return toResponse(saved);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public IncomeEstimatesResponse update(String userId, Integer id, IncomeEstimatesRequest request) {
        IncomeEstimates estimate = incomeEstimatesRepository.findByIncomeEstimatesIdAndUserId(id, userId)
                .orElseThrow(() -> new IncomeEstimatesNotFoundException(id));

        if (request.getSource() != null && !request.getSource().isBlank()) {
            estimate.setSource(request.getSource().trim());
        }
        if (request.getAmount() != null) {
            if (request.getAmount().signum() <= 0) {
                throw new IllegalArgumentException("amount must be greater than zero");
            }
            estimate.setAmount(request.getAmount());
        }
        if (request.getReceivedDate() != null) {
            estimate.setReceivedDate(request.getReceivedDate());
        }
        if (request.getMonth() != null && !request.getMonth().isBlank()) {
            estimate.setMonth(request.getMonth().trim());
        }
        if (request.getYear() != null) {
            estimate.setYear(request.getYear());
        }
        estimate.setLastUpdateTmstp(LocalDateTime.now());

        IncomeEstimates saved = incomeEstimatesRepository.save(estimate);
        logger.info("Updated income estimate id={} for userId={}", id, userId);
        return toResponse(saved);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(String userId, Integer id) {
        IncomeEstimates estimate = incomeEstimatesRepository.findByIncomeEstimatesIdAndUserId(id, userId)
                .orElseThrow(() -> new IncomeEstimatesNotFoundException(id));
        incomeEstimatesRepository.delete(estimate);
        logger.info("Deleted income estimate id={} for userId={}", id, userId);
    }

    // ─── Monthly Sync: IncomeEstimates → Income ───────────────────────────────

    /**
     * Monthly sync: copies every row in income_estimates to the income table
     * for all users, then clears the entire income_estimates table.
     *
     * Step 1 (copyIncomeEstimatesToIncome) and Step 2 (deleteAllIncomeEstimates)
     * run in separate transactions so the delete only executes after the copy is
     * fully committed to the database.
     *
     * The "incomes" cache is evicted after this method returns so users receive
     * fresh income data on their next request.
     *
     * @return number of income records inserted
     */
    @CacheEvict(cacheNames = "incomes", allEntries = true)
    public int syncAllIncomeEstimatesToIncome() {
        int inserted = copyIncomeEstimatesToIncome();
        if (inserted > 0) {
            deleteAllIncomeEstimates();
        }
        logger.info("Income estimates sync complete: {} records copied to income table, income_estimates cleared", inserted);
        return inserted;
    }

    /**
     * Transaction 1: copies all income_estimates rows into the income table.
     */
    @Transactional
    public int copyIncomeEstimatesToIncome() {
        List<IncomeEstimates> allEstimates = incomeEstimatesRepository.findAll();
        if (allEstimates.isEmpty()) {
            logger.info("No income estimates found to sync");
            return 0;
        }

        int count = 0;
        for (IncomeEstimates estimate : allEstimates) {
            Income income = new Income();
            income.setUserId(estimate.getUserId());
            income.setSource(estimate.getSource());
            income.setAmount(estimate.getAmount());
            income.setReceivedDate(estimate.getReceivedDate());
            income.setMonth(estimate.getMonth());
            income.setYear(estimate.getYear());
            income.setLastUpdateTmstp(LocalDateTime.now());
            incomeRepository.save(income);
            count++;
            logger.debug("Copied income estimate id={} to income for userId={}", estimate.getIncomeEstimatesId(), estimate.getUserId());
        }
        logger.info("Copied {} income estimate(s) to income table", count);
        return count;
    }

    /**
     * Transaction 2: deletes all rows from income_estimates after a successful copy.
     */
    @Transactional
    public void deleteAllIncomeEstimates() {
        incomeEstimatesRepository.deleteAll();
        logger.info("All income_estimates records deleted after successful sync");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void validateRequest(IncomeEstimatesRequest request, boolean isCreate) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (isCreate) {
            if (request.getAmount() == null) {
                throw new IllegalArgumentException("amount is required");
            }
            if (request.getReceivedDate() == null) {
                throw new IllegalArgumentException("receivedDate is required");
            }
            if (request.getMonth() == null || request.getMonth().isBlank()) {
                throw new IllegalArgumentException("month is required");
            }
            if (request.getYear() == null) {
                throw new IllegalArgumentException("year is required");
            }
        }
        if (request.getAmount() != null && request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }

    private IncomeEstimatesResponse toResponse(IncomeEstimates e) {
        IncomeEstimatesResponse r = new IncomeEstimatesResponse();
        r.setIncomeEstimatesId(e.getIncomeEstimatesId());
        r.setUserId(e.getUserId());
        r.setSource(e.getSource());
        r.setAmount(e.getAmount());
        r.setReceivedDate(e.getReceivedDate());
        r.setLastUpdateTmstp(e.getLastUpdateTmstp());
        r.setMonth(e.getMonth());
        r.setYear(e.getYear());
        return r;
    }
}

