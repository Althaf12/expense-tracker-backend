package com.expensetracker.controller;

import com.expensetracker.dto.IncomeDeleteRequest;
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
    public ResponseEntity<?> incomesByRange(@RequestBody Map<String, String> body) {
        logger.debug("incomesByRange called with body: {}", body);
        String userId = body.get("userId");
        String fromMonthStr = body.get("fromMonth");
        String fromYearStr = body.get("fromYear");
        String toMonthStr = body.get("toMonth");
        String toYearStr = body.get("toYear");
        int page = body.get("page") != null ? Integer.parseInt(body.get("page")) : 0;
        int size = body.get("size") != null ? Integer.parseInt(body.get("size")) : 10;

        if (userId == null || userId.isBlank()
                || fromMonthStr == null || fromYearStr == null || toMonthStr == null || toYearStr == null) {
            throw new BadRequestException("userId, fromMonth, fromYear, toMonth and toYear are required");
        }

        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }

        int fromMonth, fromYear, toMonth, toYear;
        try {
            fromMonth = Integer.parseInt(fromMonthStr);
            fromYear = Integer.parseInt(fromYearStr);
            toMonth = Integer.parseInt(toMonthStr);
            toYear = Integer.parseInt(toYearStr);
        } catch (NumberFormatException nfe) {
            throw new BadRequestException("month and year values must be integers");
        }

        // validate month ranges
        if (fromMonth < 1 || fromMonth > 12 || toMonth < 1 || toMonth > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }

        // build start and end dates: start = first day of fromMonth/fromYear, end = last day of toMonth/toYear
        LocalDate start = LocalDate.of(fromYear, fromMonth, 1);
        YearMonth ym = YearMonth.of(toYear, toMonth);
        LocalDate end = ym.atEndOfMonth();
        if (start.isAfter(end)) {
            throw new BadRequestException("from date must be before or equal to to date");
        }
        var pageResp = incomeService.getByUserAndDateRange(userId, start, end, page, size);
        logger.info("Retrieved {} incomes for userId: {} in range", pageResp.getTotalElements(), userId);
        return ResponseEntity.ok(Map.of(
                "content", pageResp.getContent(),
                "page", pageResp.getNumber(),
                "size", pageResp.getSize(),
                "totalPages", pageResp.getTotalPages(),
                "totalElements", pageResp.getTotalElements()
        ));
    }

    @PostMapping("/month")
    public ResponseEntity<?> incomesForMonth(@RequestBody Map<String, Object> body) {
        logger.debug("incomesForMonth called with body: {}", body);
        if (body == null) {
            throw new BadRequestException("request body is required");
        }
        String userId = (String) body.get("userId");
        Integer month = null;
        Integer year = null;
        int page = body.get("page") instanceof Number ? ((Number) body.get("page")).intValue() : 0;
        int size = body.get("size") instanceof Number ? ((Number) body.get("size")).intValue() : 10;
        try {
            Object m = body.get("month");
            Object y = body.get("year");
            if (m instanceof Number) month = ((Number) m).intValue();
            else if (m instanceof String) month = Integer.parseInt((String) m);
            if (y instanceof Number) year = ((Number) y).intValue();
            else if (y instanceof String) year = Integer.parseInt((String) y);
        } catch (NumberFormatException nfe) {
            throw new BadRequestException("month and year must be integers");
        }
        if (userId == null || userId.isBlank() || month == null || year == null) {
            throw new BadRequestException("userId, month and year are required");
        }
        if (month < 1 || month > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }
        if (!Constants.ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BadRequestException("Invalid page size. Allowed values: 10, 20, 50, 100");
        }
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        var pageResp = incomeService.getByUserAndDateRange(userId, start, end, page, size);
        logger.info("Retrieved {} incomes for userId: {} for {}/{}", pageResp.getTotalElements(), userId, year, month);
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
