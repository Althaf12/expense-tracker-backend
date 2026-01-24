package com.expensetracker.service;

import com.expensetracker.dto.ExpenseAdjustmentRequest;
import com.expensetracker.dto.ExpenseAdjustmentResponse;
import com.expensetracker.exception.*;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseAdjustment;
import com.expensetracker.repository.ExpenseAdjustmentRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for managing expense adjustments (refunds, cashbacks, reversals).
 */
@Service
public class ExpenseAdjustmentService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseAdjustmentService.class);

    private final ExpenseAdjustmentRepository adjustmentRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ExpenseAdjustmentService(ExpenseAdjustmentRepository adjustmentRepository,
                                    ExpenseRepository expenseRepository,
                                    UserRepository userRepository) {
        this.adjustmentRepository = adjustmentRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new expense adjustment.
     */
    @Transactional
    @CacheEvict(value = "expenses", allEntries = true)
    public ExpenseAdjustmentResponse createAdjustment(ExpenseAdjustmentRequest request) {
        logger.info("Creating expense adjustment for userId: {}, expenseId: {}",
                request.getUserId(), request.getExpensesId());

        // Validate user exists
        validateUserExists(request.getUserId());

        // Validate expense exists and belongs to user
        Expense expense = validateExpenseForUser(request.getExpensesId(), request.getUserId());

        // Validate adjustment type and status
        validateAdjustmentType(request.getAdjustmentType());
        String status = request.getStatus() != null ? request.getStatus() : "PENDING";
        validateAdjustmentStatus(status);

        // Validate adjustment amount
        validateAdjustmentAmount(request.getAdjustmentAmount());

        // Validate adjustment amount does not exceed expense amount
        validateAdjustmentAmountAgainstExpense(request.getAdjustmentAmount(), expense, null);

        // Validate adjustment date
        validateAdjustmentDate(request.getAdjustmentDate());

        ExpenseAdjustment adjustment = new ExpenseAdjustment();
        adjustment.setExpensesId(request.getExpensesId());
        adjustment.setUserId(request.getUserId());
        adjustment.setAdjustmentType(request.getAdjustmentType().toUpperCase());
        adjustment.setAdjustmentAmount(request.getAdjustmentAmount());
        adjustment.setAdjustmentReason(request.getAdjustmentReason());
        adjustment.setAdjustmentDate(request.getAdjustmentDate() != null ?
                request.getAdjustmentDate() : LocalDate.now());
        adjustment.setStatus(status.toUpperCase());

        ExpenseAdjustment saved = adjustmentRepository.save(adjustment);
        logger.info("Created expense adjustment with ID: {}", saved.getExpenseAdjustmentsId());

        return mapToResponse(saved, expense);
    }

    /**
     * Update an existing expense adjustment.
     */
    @Transactional
    @CacheEvict(value = "expenses", allEntries = true)
    public ExpenseAdjustmentResponse updateAdjustment(ExpenseAdjustmentRequest request) {
        logger.info("Updating expense adjustment ID: {}", request.getExpenseAdjustmentsId());

        if (request.getExpenseAdjustmentsId() == null) {
            throw new BadRequestException("expenseAdjustmentsId is required for update");
        }

        ExpenseAdjustment existing = adjustmentRepository.findById(request.getExpenseAdjustmentsId())
                .orElseThrow(() -> new ExpenseAdjustmentNotFoundException(request.getExpenseAdjustmentsId()));

        // Validate user owns this adjustment
        if (!existing.getUserId().equals(request.getUserId())) {
            throw new BadRequestException("User ID mismatch - you cannot update another user's adjustment");
        }

        // Get expense for validation
        Expense expense = expenseRepository.findById(existing.getExpensesId())
                .orElseThrow(() -> new ExpenseNotFoundException(existing.getExpensesId()));

        // Update fields if provided
        if (request.getAdjustmentType() != null) {
            validateAdjustmentType(request.getAdjustmentType());
            existing.setAdjustmentType(request.getAdjustmentType().toUpperCase());
        }
        if (request.getAdjustmentAmount() != null) {
            validateAdjustmentAmount(request.getAdjustmentAmount());
            // Validate adjustment amount does not exceed expense amount (excluding current adjustment)
            validateAdjustmentAmountAgainstExpense(request.getAdjustmentAmount(), expense, existing.getExpenseAdjustmentsId());
            existing.setAdjustmentAmount(request.getAdjustmentAmount());
        }
        if (request.getAdjustmentReason() != null) {
            existing.setAdjustmentReason(request.getAdjustmentReason());
        }
        if (request.getAdjustmentDate() != null) {
            validateAdjustmentDate(request.getAdjustmentDate());
            existing.setAdjustmentDate(request.getAdjustmentDate());
        }
        if (request.getStatus() != null) {
            validateAdjustmentStatus(request.getStatus());
            existing.setStatus(request.getStatus().toUpperCase());
        }

        ExpenseAdjustment saved = adjustmentRepository.save(existing);
        logger.info("Updated expense adjustment ID: {}", saved.getExpenseAdjustmentsId());

        return mapToResponse(saved, expense);
    }

    /**
     * Delete an expense adjustment.
     */
    @Transactional
    @CacheEvict(value = "expenses", allEntries = true)
    public boolean deleteAdjustment(String userId, Integer adjustmentId) {
        logger.info("Deleting expense adjustment ID: {} for userId: {}", adjustmentId, userId);

        if (userId == null || adjustmentId == null) {
            throw new BadRequestException("userId and adjustmentId are required");
        }

        ExpenseAdjustment existing = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new ExpenseAdjustmentNotFoundException(adjustmentId));

        if (!existing.getUserId().equals(userId)) {
            throw new BadRequestException("User ID mismatch - you cannot delete another user's adjustment");
        }

        adjustmentRepository.deleteById(adjustmentId);
        logger.info("Deleted expense adjustment ID: {}", adjustmentId);
        return true;
    }

    /**
     * Get adjustment by ID.
     */
    public ExpenseAdjustmentResponse getAdjustmentById(String userId, Integer adjustmentId) {
        logger.debug("Fetching adjustment ID: {} for userId: {}", adjustmentId, userId);

        ExpenseAdjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new ExpenseAdjustmentNotFoundException(adjustmentId));

        if (!adjustment.getUserId().equals(userId)) {
            throw new BadRequestException("User ID mismatch - you cannot view another user's adjustment");
        }

        Expense expense = expenseRepository.findById(adjustment.getExpensesId()).orElse(null);
        return mapToResponse(adjustment, expense);
    }

    /**
     * Get all adjustments for a user with pagination.
     */
    public Page<ExpenseAdjustmentResponse> getAdjustmentsByUserId(String userId, int page, int size) {
        logger.debug("Fetching adjustments for userId: {}, page: {}, size: {}", userId, page, size);

        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: " + Constants.ALLOWED_PAGE_SIZES);
        }

        validateUserExists(userId);

        PageRequest pageRequest = PageRequest.of(Math.max(0, page), size);
        Page<ExpenseAdjustment> adjustmentPage = adjustmentRepository.findByUserId(userId, pageRequest);

        List<ExpenseAdjustmentResponse> responses = mapToResponses(adjustmentPage.getContent());
        return new PageImpl<>(responses, pageRequest, adjustmentPage.getTotalElements());
    }

    /**
     * Get all adjustments for a specific expense.
     */
    public List<ExpenseAdjustmentResponse> getAdjustmentsByExpenseId(String userId, Integer expenseId) {
        logger.debug("Fetching adjustments for expenseId: {}", expenseId);

        // Validate expense belongs to user
        Expense expense = validateExpenseForUser(expenseId, userId);

        List<ExpenseAdjustment> adjustments = adjustmentRepository.findByExpensesId(expenseId);
        return adjustments.stream()
                .map(adj -> mapToResponse(adj, expense))
                .collect(Collectors.toList());
    }

    /**
     * Get adjustments for a user within a date range with pagination.
     */
    public Page<ExpenseAdjustmentResponse> getAdjustmentsByUserIdAndDateRange(
            String userId, LocalDate start, LocalDate end, int page, int size) {
        logger.debug("Fetching adjustments for userId: {} from {} to {}", userId, start, end);

        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: " + Constants.ALLOWED_PAGE_SIZES);
        }

        validateUserExists(userId);

        PageRequest pageRequest = PageRequest.of(Math.max(0, page), size);
        Page<ExpenseAdjustment> adjustmentPage = adjustmentRepository
                .findByUserIdAndAdjustmentDateBetween(userId, start, end, pageRequest);

        List<ExpenseAdjustmentResponse> responses = mapToResponses(adjustmentPage.getContent());
        return new PageImpl<>(responses, pageRequest, adjustmentPage.getTotalElements());
    }

    /**
     * Get total completed adjustment amount for a specific expense.
     */
    public BigDecimal getTotalCompletedAdjustmentForExpense(Integer expenseId) {
        return adjustmentRepository.getTotalCompletedAdjustmentForExpense(expenseId);
    }

    /**
     * Get total completed adjustments for a user within a date range.
     */
    public BigDecimal getTotalCompletedAdjustmentsForUserInRange(String userId, LocalDate start, LocalDate end) {
        return adjustmentRepository.getTotalCompletedAdjustmentsForUserInRange(userId, start, end);
    }

    /**
     * Get total completed adjustments for a user in a specific month.
     */
    public BigDecimal getTotalCompletedAdjustmentsForMonth(String userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return getTotalCompletedAdjustmentsForUserInRange(userId, start, end);
    }

    /**
     * Get a map of expense ID to total completed adjustment amount for multiple expenses.
     */
    public Map<Integer, BigDecimal> getCompletedAdjustmentsMapForExpenses(List<Integer> expenseIds) {
        if (expenseIds == null || expenseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ExpenseAdjustment> completedAdjustments = adjustmentRepository
                .findCompletedAdjustmentsForExpenses(expenseIds);

        return completedAdjustments.stream()
                .collect(Collectors.groupingBy(
                        ExpenseAdjustment::getExpensesId,
                        Collectors.reducing(BigDecimal.ZERO,
                                ExpenseAdjustment::getAdjustmentAmount,
                                BigDecimal::add)
                ));
    }

    // ============ Private helper methods ============

    private void validateUserExists(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }

    private Expense validateExpenseForUser(Integer expenseId, String userId) {
        if (expenseId == null) {
            throw new BadRequestException("expensesId is required");
        }
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));
        if (!expense.getUserId().equals(userId)) {
            throw new BadRequestException("Expense does not belong to this user");
        }
        return expense;
    }

    private void validateAdjustmentType(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("adjustmentType is required");
        }
        if (!Constants.VALID_ADJUSTMENT_TYPES.contains(type.toUpperCase())) {
            throw new InvalidAdjustmentTypeException(type);
        }
    }

    private void validateAdjustmentStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }
        if (!Constants.VALID_ADJUSTMENT_STATUSES.contains(status.toUpperCase())) {
            throw new InvalidAdjustmentStatusException(status);
        }
    }

    private void validateAdjustmentAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("adjustmentAmount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidExpenseAmountException("Adjustment amount must be greater than zero");
        }
        // Validate decimal places (max 2)
        if (amount.scale() > 2) {
            throw new InvalidExpenseAmountException("Adjustment amount can have at most 2 decimal places");
        }
    }

    private void validateAdjustmentDate(LocalDate date) {
        if (date == null) {
            return; // Will default to current date
        }
        int year = date.getYear();
        if (year < 2000 || year > 2100) {
            throw new InvalidExpenseDateException(date);
        }
    }

    /**
     * Validates that the adjustment amount does not exceed the expense amount,
     * and that total adjustments (existing + new) do not exceed the expense amount.
     *
     * @param newAdjustmentAmount the new adjustment amount to validate
     * @param expense the expense being adjusted
     * @param excludeAdjustmentId adjustment ID to exclude from total calculation (for updates), null for new adjustments
     * @throws AdjustmentAmountExceedsExpenseException if validation fails
     */
    private void validateAdjustmentAmountAgainstExpense(BigDecimal newAdjustmentAmount, Expense expense, Integer excludeAdjustmentId) {
        BigDecimal expenseAmount = expense.getExpenseAmount();
        if (expenseAmount == null) {
            expenseAmount = BigDecimal.ZERO;
        }

        // Check 1: New adjustment amount should not exceed expense amount
        if (newAdjustmentAmount.compareTo(expenseAmount) > 0) {
            throw new AdjustmentAmountExceedsExpenseException(newAdjustmentAmount, expenseAmount);
        }

        // Check 2: Get existing adjustments for this expense and calculate total
        List<ExpenseAdjustment> existingAdjustments = adjustmentRepository.findByExpensesId(expense.getExpensesId());

        BigDecimal totalExistingAdjustments = BigDecimal.ZERO;
        for (ExpenseAdjustment adj : existingAdjustments) {
            // Exclude the current adjustment being updated (if any)
            if (excludeAdjustmentId != null && excludeAdjustmentId.equals(adj.getExpenseAdjustmentsId())) {
                continue;
            }
            if (adj.getAdjustmentAmount() != null) {
                totalExistingAdjustments = totalExistingAdjustments.add(adj.getAdjustmentAmount());
            }
        }

        // Check 3: Total adjustments (existing + new) should not exceed expense amount
        BigDecimal totalAdjustments = totalExistingAdjustments.add(newAdjustmentAmount);
        if (totalAdjustments.compareTo(expenseAmount) > 0) {
            throw new AdjustmentAmountExceedsExpenseException(newAdjustmentAmount, totalExistingAdjustments, expenseAmount);
        }

        logger.debug("Adjustment amount validation passed: expense={}, existing={}, new={}, total={}",
                expenseAmount, totalExistingAdjustments, newAdjustmentAmount, totalAdjustments);
    }

    private ExpenseAdjustmentResponse mapToResponse(ExpenseAdjustment adjustment, Expense expense) {
        ExpenseAdjustmentResponse response = new ExpenseAdjustmentResponse();
        response.setExpenseAdjustmentsId(adjustment.getExpenseAdjustmentsId());
        response.setExpensesId(adjustment.getExpensesId());
        response.setUserId(adjustment.getUserId());
        response.setAdjustmentType(adjustment.getAdjustmentType());
        response.setAdjustmentAmount(adjustment.getAdjustmentAmount());
        response.setAdjustmentReason(adjustment.getAdjustmentReason());
        response.setAdjustmentDate(adjustment.getAdjustmentDate());
        response.setStatus(adjustment.getStatus());
        response.setCreatedAt(adjustment.getCreatedAt());
        response.setLastUpdateTmstp(adjustment.getLastUpdateTmstp());

        if (expense != null) {
            response.setExpenseName(expense.getExpenseName());
            response.setOriginalExpenseAmount(expense.getExpenseAmount());
        }

        return response;
    }

    private List<ExpenseAdjustmentResponse> mapToResponses(List<ExpenseAdjustment> adjustments) {
        if (adjustments == null || adjustments.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect all expense IDs
        Set<Integer> expenseIds = adjustments.stream()
                .map(ExpenseAdjustment::getExpensesId)
                .collect(Collectors.toSet());

        // Batch fetch expenses
        List<Expense> expenses = expenseRepository.findAllById(expenseIds);
        Map<Integer, Expense> expenseMap = expenses.stream()
                .collect(Collectors.toMap(Expense::getExpensesId, e -> e));

        return adjustments.stream()
                .map(adj -> mapToResponse(adj, expenseMap.get(adj.getExpensesId())))
                .collect(Collectors.toList());
    }
}
