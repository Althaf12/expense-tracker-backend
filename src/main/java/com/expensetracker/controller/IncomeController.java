package com.expensetracker.controller;

import com.expensetracker.dto.IncomeDeleteRequest;
import com.expensetracker.dto.IncomePageRequest;
import com.expensetracker.dto.IncomeRequest;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.exception.UserNotFoundException;
import com.expensetracker.service.IncomeService;
import com.expensetracker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import com.expensetracker.util.Constants;

@RestController
@RequestMapping("/api/income")
public class IncomeController {

    private static final Logger logger = LoggerFactory.getLogger(IncomeController.class);

    private final IncomeService incomeService;
    private final UserService userService;

    @Autowired
    public IncomeController(IncomeService incomeService, UserService userService) {
        this.incomeService = incomeService;
        this.userService = userService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addIncome(@RequestBody IncomeRequest request) {
        logger.debug("addIncome called with request: {}", request);
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (userService.findById(request.getUserId()).isEmpty()) {
            throw new UserNotFoundException(request.getUserId());
        }
        com.expensetracker.model.Income inc = new com.expensetracker.model.Income();
        inc.setUserId(request.getUserId());
        inc.setSource(request.getSource() == null ? "Salary" : request.getSource());
        inc.setAmount(request.getAmount());
        inc.setReceivedDate(request.getReceivedDate());
        inc.setMonth(request.getMonth());
        inc.setYear(request.getYear());
        incomeService.addIncome(inc);
        logger.info("Income added for userId: {}", request.getUserId());
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/range")
    public ResponseEntity<?> incomesByRange(@RequestBody IncomePageRequest req) {
        logger.debug("incomesByRange called with request: {}", req);
        if (req == null || req.getUserId() == null || req.getUserId().isBlank()
                || req.getFromMonth() == null || req.getFromYear() == null
                || req.getToMonth() == null || req.getToYear() == null) {
            throw new BadRequestException("userId, fromMonth, fromYear, toMonth and toYear are required");
        }
        int size = req.getSize() != null ? req.getSize() : 10;
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        req.setSize(size);

        int fromMonth, fromYear, toMonth, toYear;
        try {
            fromMonth = Integer.parseInt(req.getFromMonth());
            fromYear  = Integer.parseInt(req.getFromYear());
            toMonth   = Integer.parseInt(req.getToMonth());
            toYear    = Integer.parseInt(req.getToYear());
        } catch (NumberFormatException nfe) {
            throw new BadRequestException("month and year values must be integers");
        }
        if (fromMonth < 1 || fromMonth > 12 || toMonth < 1 || toMonth > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }

        LocalDate start = LocalDate.of(fromYear, fromMonth, 1);
        YearMonth ym    = YearMonth.of(toYear, toMonth);
        LocalDate end   = ym.atEndOfMonth();
        if (start.isAfter(end)) {
            throw new BadRequestException("from date must be before or equal to to date");
        }

        var pageResp = incomeService.getFilteredIncomes(req.getUserId(), start, end, req);
        logger.info("Retrieved {} incomes for userId: {} in range", pageResp.getTotalElements(), req.getUserId());
        return ResponseEntity.ok(Map.of(
                "content", pageResp.getContent(),
                "page", pageResp.getNumber(),
                "size", pageResp.getSize(),
                "totalPages", pageResp.getTotalPages(),
                "totalElements", pageResp.getTotalElements()
        ));
    }

    @PostMapping("/month")
    public ResponseEntity<?> incomesForMonth(@RequestBody IncomePageRequest req) {
        logger.debug("incomesForMonth called with request: {}", req);
        if (req == null || req.getUserId() == null || req.getUserId().isBlank()
                || req.getMonth() == null || req.getYear() == null) {
            throw new BadRequestException("userId, month and year are required");
        }
        if (req.getMonth() < 1 || req.getMonth() > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }
        int size = req.getSize() != null ? req.getSize() : 10;
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        req.setSize(size);

        YearMonth ym  = YearMonth.of(req.getYear(), req.getMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();

        var pageResp = incomeService.getFilteredIncomes(req.getUserId(), start, end, req);
        logger.info("Retrieved {} incomes for userId: {} for {}/{}",
                pageResp.getTotalElements(), req.getUserId(), req.getYear(), req.getMonth());
        return ResponseEntity.ok(Map.of(
                "content", pageResp.getContent(),
                "page", pageResp.getNumber(),
                "size", pageResp.getSize(),
                "totalPages", pageResp.getTotalPages(),
                "totalElements", pageResp.getTotalElements()
        ));
    }

    @PostMapping("/year")
    public ResponseEntity<?> incomesForYear(@RequestBody IncomePageRequest req) {
        logger.debug("incomesForYear called with request: {}", req);
        if (req == null || req.getUserId() == null || req.getUserId().isBlank()
                || req.getYear() == null) {
            throw new BadRequestException("userId and year are required");
        }
        int size = req.getSize() != null ? req.getSize() : 10;
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        req.setSize(size);

        LocalDate start = LocalDate.of(req.getYear(), 1, 1);
        LocalDate end   = LocalDate.of(req.getYear(), 12, 31);

        var pageResp = incomeService.getFilteredIncomes(req.getUserId(), start, end, req);
        logger.info("Retrieved {} incomes for userId: {} for year {}",
                pageResp.getTotalElements(), req.getUserId(), req.getYear());
        return ResponseEntity.ok(Map.of(
                "content", pageResp.getContent(),
                "page", pageResp.getNumber(),
                "size", pageResp.getSize(),
                "totalPages", pageResp.getTotalPages(),
                "totalElements", pageResp.getTotalElements()
        ));
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteIncome(@RequestBody IncomeDeleteRequest request) {
        logger.debug("deleteIncome called with request: {}", request);
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (request.getIncomeId() == null) {
            throw new BadRequestException("incomeId is required");
        }
        boolean deleted = incomeService.deleteIncome(request.getUserId(), request.getIncomeId());
        if (!deleted) {
            throw new ResourceNotFoundException("Income not found or userId mismatch for incomeId: " + request.getIncomeId());
        }
        logger.info("Income deleted: incomeId={}, userId={}", request.getIncomeId(), request.getUserId());
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @DeleteMapping("/{incomeId}")
    public ResponseEntity<?> deleteIncomeById(@PathVariable Integer incomeId) {
        logger.debug("deleteIncomeById called for incomeId: {}", incomeId);
        incomeService.deleteIncome(incomeId);
        logger.info("Income deleted: incomeId={}", incomeId);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateIncome(@RequestBody com.expensetracker.dto.IncomeUpdateRequest request) {
        logger.debug("updateIncome called with request: {}", request);
        if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BadRequestException("userId is required");
        }
        if (request.getIncomeId() == null) {
            throw new BadRequestException("incomeId is required");
        }
        // ensure user exists
        if (userService.findById(request.getUserId()).isEmpty()) {
            throw new UserNotFoundException(request.getUserId());
        }
        com.expensetracker.model.Income upd = new com.expensetracker.model.Income();
        upd.setUserId(request.getUserId());
        upd.setSource(request.getSource());
        upd.setAmount(request.getAmount());
        upd.setReceivedDate(request.getReceivedDate());
        incomeService.updateIncome(request.getIncomeId(), request.getUserId(), upd);
        logger.info("Income updated: incomeId={}", request.getIncomeId());
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
