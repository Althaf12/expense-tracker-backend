package com.expensetracker.service;

import com.expensetracker.dto.UserExpensesEstimatesRequest;
import com.expensetracker.dto.UserExpensesEstimatesResponse;
import com.expensetracker.exception.UserExpensesEstimatesNotFoundException;
import com.expensetracker.model.UserCreditCardEstimates;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.model.UserExpenses;
import com.expensetracker.model.UserExpensesEstimates;
import com.expensetracker.repository.UserCreditCardEstimatesRepository;
import com.expensetracker.repository.UserExpenseCategoryRepository;
import com.expensetracker.repository.UserExpensesEstimatesRepository;
import com.expensetracker.repository.UserExpensesRepository;
import com.expensetracker.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserExpensesEstimatesService {

    private static final Logger logger = LoggerFactory.getLogger(UserExpensesEstimatesService.class);

    private final UserExpensesEstimatesRepository estimatesRepository;
    private final UserExpenseCategoryRepository userExpenseCategoryRepository;
    private final UserExpensesRepository userExpensesRepository;
    private final UserCreditCardEstimatesRepository creditCardEstimatesRepository;

    @Autowired
    public UserExpensesEstimatesService(UserExpensesEstimatesRepository estimatesRepository,
                                        UserExpenseCategoryRepository userExpenseCategoryRepository,
                                        UserExpensesRepository userExpensesRepository,
                                        UserCreditCardEstimatesRepository creditCardEstimatesRepository) {
        this.estimatesRepository = estimatesRepository;
        this.userExpenseCategoryRepository = userExpenseCategoryRepository;
        this.userExpensesRepository = userExpensesRepository;
        this.creditCardEstimatesRepository = creditCardEstimatesRepository;
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    public List<UserExpensesEstimatesResponse> findAll(String userId) {
        return estimatesRepository.findByUserIdOrderByUserExpenseName(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<UserExpensesEstimatesResponse> findActive(String userId) {
        return estimatesRepository.findByUserIdAndStatusOrderByUserExpenseName(userId, "A")
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public UserExpensesEstimatesResponse add(String userId, UserExpensesEstimatesRequest request) {
        if (request.getUserExpenseName() == null || request.getUserExpenseName().isBlank()) {
            throw new IllegalArgumentException("userExpenseName is required");
        }
        if (request.getUserExpenseCategoryId() == null) {
            throw new IllegalArgumentException("userExpenseCategoryId is required");
        }

        UserExpensesEstimates estimate = new UserExpensesEstimates();
        estimate.setUserId(userId);
        estimate.setUserExpenseName(request.getUserExpenseName().trim());
        estimate.setUserExpenseCategoryId(request.getUserExpenseCategoryId());
        estimate.setAmount(request.getAmount());
        estimate.setStatus(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus() : "A");
        estimate.setLastUpdateTmstp(LocalDateTime.now());

        UserExpensesEstimates saved = estimatesRepository.save(estimate);
        logger.info("Added expense estimate id={} for userId={}", saved.getUserExpensesEstimatesId(), userId);
        return toResponse(saved);
    }

    @Transactional
    public UserExpensesEstimatesResponse update(String userId, Integer id, UserExpensesEstimatesRequest request) {
        UserExpensesEstimates estimate = estimatesRepository.findByUserExpensesEstimatesIdAndUserId(id, userId)
                .orElseThrow(() -> new UserExpensesEstimatesNotFoundException(id));

        if (request.getUserExpenseName() != null && !request.getUserExpenseName().isBlank()) {
            estimate.setUserExpenseName(request.getUserExpenseName().trim());
        }
        if (request.getUserExpenseCategoryId() != null) {
            estimate.setUserExpenseCategoryId(request.getUserExpenseCategoryId());
        }
        if (request.getAmount() != null) {
            estimate.setAmount(request.getAmount());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            estimate.setStatus(request.getStatus());
        }
        estimate.setLastUpdateTmstp(LocalDateTime.now());

        UserExpensesEstimates saved = estimatesRepository.save(estimate);
        logger.info("Updated expense estimate id={} for userId={}", id, userId);
        return toResponse(saved);
    }

    @Transactional
    public void delete(String userId, Integer id) {
        UserExpensesEstimates estimate = estimatesRepository.findByUserExpensesEstimatesIdAndUserId(id, userId)
                .orElseThrow(() -> new UserExpensesEstimatesNotFoundException(id));
        estimatesRepository.delete(estimate);
        logger.info("Deleted expense estimate id={} for userId={}", id, userId);
    }

    // ─── Sync: Estimates → UserExpenses ──────────────────────────────────────

    /**
     * Runs at the start of each month for all users.
     * Syncs UserExpensesEstimates → UserExpenses:
     *  - Active estimates: insert if missing, update if changed, skip if unchanged.
     *  - Inactive/deleted estimates: delete the matching UserExpenses record.
     * Also syncs UserCreditCardEstimates → UserExpenses under a "Credit Card" category.
     *
     * After the sync completes, the userExpenses and expenses caches are fully evicted
     * so users receive fresh data on their next request.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "userExpenses", allEntries = true),
            @CacheEvict(cacheNames = "expenses", allEntries = true)
    })
    public void syncAllUsersEstimatesToUserExpenses() {
        // --- Sync regular estimates ---
        List<String> estimateUserIds = estimatesRepository.findDistinctUserIds();
        logger.info("Starting monthly estimates sync for {} users (regular)", estimateUserIds.size());
        int totalInserted = 0, totalUpdated = 0, totalDeactivated = 0;

        for (String userId : estimateUserIds) {
            int[] counts = syncUserEstimatesToUserExpenses(userId);
            totalInserted += counts[0];
            totalUpdated += counts[1];
            totalDeactivated += counts[2];
        }
        logger.info("Monthly regular estimates sync complete: inserted={}, updated={}, deleted={}",
                totalInserted, totalUpdated, totalDeactivated);

        // --- Sync credit card estimates ---
        // Union: users who have CC estimates + users who have a "Credit Card" category
        // (the latter ensures tombstone runs even when all CC estimates were deleted).
        Set<String> ccUserIdSet = new HashSet<>(creditCardEstimatesRepository.findDistinctUserIds());
        ccUserIdSet.addAll(userExpenseCategoryRepository
                .findDistinctUserIdsByCategoryNameIgnoreCase(Constants.CREDIT_CARD_CATEGORY_NAME));
        logger.info("Starting monthly credit card estimates sync for {} users", ccUserIdSet.size());
        int ccInserted = 0, ccUpdated = 0, ccDeleted = 0;

        for (String userId : ccUserIdSet) {
            int[] counts = syncUserCreditCardEstimatesToUserExpenses(userId);
            ccInserted += counts[0];
            ccUpdated += counts[1];
            ccDeleted += counts[2];
        }
        logger.info("Monthly credit card estimates sync complete: inserted={}, updated={}, deleted={}",
                ccInserted, ccUpdated, ccDeleted);
    }

    /**
     * Syncs estimates for a single user. Returns [inserted, updated, deactivated].
     *
     * Pass 1 – per-estimate: insert/update active ones, deactivate user_expenses for inactive ones.
     * Pass 2 – tombstone: deactivate any active user_expense in an estimates-managed category
     *           whose (name, categoryId) no longer exists as an active estimate (i.e. was deleted).
     */
    @Transactional
    public int[] syncUserEstimatesToUserExpenses(String userId) {
        List<UserExpensesEstimates> estimates = estimatesRepository.findByUserIdOrderByUserExpenseName(userId);
        int inserted = 0, updated = 0, deactivated = 0;

        // ── Pass 1: process each estimate ────────────────────────────────────
        for (UserExpensesEstimates estimate : estimates) {
            if (estimate.getUserExpenseName() == null || estimate.getUserExpenseCategoryId() == null) {
                continue;
            }

            Optional<UserExpenses> existingOpt = userExpensesRepository
                    .findByUserIdAndUserExpenseNameIgnoreCaseAndUserExpenseCategoryId(
                            userId,
                            estimate.getUserExpenseName(),
                            estimate.getUserExpenseCategoryId());

            if ("A".equals(estimate.getStatus())) {
                if (existingOpt.isEmpty()) {
                    // Insert
                    UserExpenses ue = new UserExpenses();
                    ue.setUserId(userId);
                    ue.setUserExpenseName(estimate.getUserExpenseName());
                    ue.setUserExpenseCategoryId(estimate.getUserExpenseCategoryId());
                    ue.setAmount(estimate.getAmount());
                    ue.setPaid("N");
                    ue.setStatus("A");
                    ue.setLastUpdateTmstp(LocalDateTime.now());
                    userExpensesRepository.save(ue);
                    inserted++;
                    logger.debug("Inserted user_expense '{}' for userId={}", estimate.getUserExpenseName(), userId);
                } else {
                    // Update only if something changed
                    UserExpenses ue = existingOpt.get();
                    boolean changed = false;

                    if (!Objects.equals(ue.getAmount(), estimate.getAmount())) {
                        ue.setAmount(estimate.getAmount());
                        changed = true;
                    }
                    if (!"A".equals(ue.getStatus())) {
                        ue.setStatus("A");
                        changed = true;
                    }
                    if (changed) {
                        ue.setLastUpdateTmstp(LocalDateTime.now());
                        userExpensesRepository.save(ue);
                        updated++;
                        logger.debug("Updated user_expense '{}' for userId={}", estimate.getUserExpenseName(), userId);
                    }
                }
            } else {
                // Estimate is inactive → delete the matching user_expense if it exists
                if (existingOpt.isPresent()) {
                    userExpensesRepository.delete(existingOpt.get());
                    deactivated++;
                    logger.debug("Deleted user_expense '{}' (inactive estimate) for userId={}",
                            estimate.getUserExpenseName(), userId);
                }
            }
        }

        // ── Pass 2: tombstone – remove orphaned user_expenses ─────────────────
        // Build a set of (name_lower|categoryId) keys for all ACTIVE estimates.
        Set<String> activeEstimateKeys = estimates.stream()
                .filter(e -> "A".equals(e.getStatus())
                        && e.getUserExpenseName() != null
                        && e.getUserExpenseCategoryId() != null)
                .map(e -> e.getUserExpenseName().toLowerCase() + "|" + e.getUserExpenseCategoryId())
                .collect(Collectors.toSet());

        // All category IDs that are managed by estimates for this user (active OR inactive).
        Set<Integer> managedCategoryIds = estimates.stream()
                .filter(e -> e.getUserExpenseCategoryId() != null)
                .map(UserExpensesEstimates::getUserExpenseCategoryId)
                .collect(Collectors.toSet());

        if (!managedCategoryIds.isEmpty()) {
            // Find all currently-active user_expenses in those categories.
            List<UserExpenses> activeInCategories = userExpensesRepository
                    .findByUserIdAndStatusAndUserExpenseCategoryIdIn(userId, "A", managedCategoryIds);

            for (UserExpenses ue : activeInCategories) {
                if (ue.getUserExpenseName() == null) continue;
                String key = ue.getUserExpenseName().toLowerCase() + "|" + ue.getUserExpenseCategoryId();
                if (!activeEstimateKeys.contains(key)) {
                    // No longer backed by an active estimate — delete it.
                    userExpensesRepository.delete(ue);
                    deactivated++;
                    logger.debug("Deleted orphaned user_expense '{}' (no matching active estimate) for userId={}",
                            ue.getUserExpenseName(), userId);
                }
            }
        }

        logger.info("Estimates sync for userId={}: inserted={}, updated={}, deleted={}",
                userId, inserted, updated, deactivated);
        return new int[]{inserted, updated, deactivated};
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    /**
     * Syncs credit card estimates for a single user into user_expenses under
     * the "Credit Card" category. Returns [inserted, updated, deactivated].
     *
     * Matching key: userId + cardName + creditCardCategoryId.
     * All credit card estimate records are treated as active (no status column).
     *
     * Pass 1 – per-estimate: insert new / update changed.
     * Pass 2 – tombstone: deactivate any active user_expense in the Credit Card category
     *           whose cardName no longer exists in credit card estimates (i.e. was deleted).
     */
    @Transactional
    public int[] syncUserCreditCardEstimatesToUserExpenses(String userId) {
        List<UserCreditCardEstimates> ccEstimates = creditCardEstimatesRepository.findByUserIdOrderByCardName(userId);

        Optional<UserExpenseCategory> creditCardCategoryOpt = userExpenseCategoryRepository
                .findByUserIdAndUserExpenseCategoryNameIgnoreCase(userId, Constants.CREDIT_CARD_CATEGORY_NAME);
        if (creditCardCategoryOpt.isEmpty()) {
            logger.info("No '{}' category found for userId={}, skipping credit card estimates sync",
                    Constants.CREDIT_CARD_CATEGORY_NAME, userId);
            return new int[]{0, 0, 0};
        }
        Integer creditCardCategoryId = creditCardCategoryOpt.get().getUserExpenseCategoryId();

        int inserted = 0, updated = 0, deactivated = 0;

        // ── Pass 1: process each CC estimate ─────────────────────────────────
        for (UserCreditCardEstimates cc : ccEstimates) {
            if (cc.getCardName() == null || cc.getCardName().isBlank()) {
                continue;
            }

            Optional<UserExpenses> existingOpt = userExpensesRepository
                    .findByUserIdAndUserExpenseNameIgnoreCaseAndUserExpenseCategoryId(
                            userId, cc.getCardName(), creditCardCategoryId);

            if (existingOpt.isEmpty()) {
                // INSERT
                UserExpenses ue = new UserExpenses();
                ue.setUserId(userId);
                ue.setUserExpenseName(cc.getCardName());
                ue.setUserExpenseCategoryId(creditCardCategoryId);
                ue.setAmount(cc.getAmount());
                ue.setPaid("N");
                ue.setStatus("A");
                ue.setLastUpdateTmstp(LocalDateTime.now());
                userExpensesRepository.save(ue);
                inserted++;
                logger.debug("Inserted credit card user_expense '{}' for userId={}", cc.getCardName(), userId);
            } else {
                // UPDATE if amount changed or record was inactive
                UserExpenses ue = existingOpt.get();
                boolean changed = false;

                if (!Objects.equals(ue.getAmount(), cc.getAmount())) {
                    ue.setAmount(cc.getAmount());
                    changed = true;
                }
                if (!"A".equals(ue.getStatus())) {
                    ue.setStatus("A");
                    changed = true;
                }
                if (changed) {
                    ue.setLastUpdateTmstp(LocalDateTime.now());
                    userExpensesRepository.save(ue);
                    updated++;
                    logger.debug("Updated credit card user_expense '{}' for userId={}", cc.getCardName(), userId);
                }
            }
        }

        // ── Pass 2: tombstone – deactivate CC user_expenses with no matching estimate ──
        Set<String> activeCardNames = ccEstimates.stream()
                .filter(cc -> cc.getCardName() != null && !cc.getCardName().isBlank())
                .map(cc -> cc.getCardName().toLowerCase())
                .collect(Collectors.toSet());

        List<UserExpenses> activeCcExpenses = userExpensesRepository
                .findByUserIdAndStatusAndUserExpenseCategoryId(userId, "A", creditCardCategoryId);

        for (UserExpenses ue : activeCcExpenses) {
            if (ue.getUserExpenseName() == null) continue;
            if (!activeCardNames.contains(ue.getUserExpenseName().toLowerCase())) {
                userExpensesRepository.delete(ue);
                deactivated++;
                logger.debug("Deleted orphaned CC user_expense '{}' (no matching CC estimate) for userId={}",
                        ue.getUserExpenseName(), userId);
            }
        }

        logger.info("Credit card estimates sync for userId={}: inserted={}, updated={}, deleted={}",
                userId, inserted, updated, deactivated);
        return new int[]{inserted, updated, deactivated};
    }

    private UserExpensesEstimatesResponse toResponse(UserExpensesEstimates e) {
        UserExpensesEstimatesResponse r = new UserExpensesEstimatesResponse();
        r.setUserExpensesEstimatesId(e.getUserExpensesEstimatesId());
        r.setUserId(e.getUserId());
        r.setUserExpenseName(e.getUserExpenseName());
        r.setUserExpenseCategoryId(e.getUserExpenseCategoryId());

        String catName = null;
        if (e.getUserExpenseCategoryId() != null) {
            Optional<UserExpenseCategory> catOpt = userExpenseCategoryRepository.findById(e.getUserExpenseCategoryId());
            if (catOpt.isPresent()) catName = catOpt.get().getUserExpenseCategoryName();
        }
        r.setUserExpenseCategoryName(catName);
        r.setAmount(e.getAmount());
        r.setLastUpdateTmstp(e.getLastUpdateTmstp());
        r.setStatus(e.getStatus());
        return r;
    }
}


