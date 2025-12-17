package com.expensetracker.controller;

import com.expensetracker.dto.IncomeDeleteRequest;
import com.expensetracker.dto.IncomeRequest;
import com.expensetracker.service.IncomeService;
import com.expensetracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/income")
public class IncomeController {

    private final IncomeService incomeService;
    private final UserService userService;
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10,20,50,100);

    @Autowired
    public IncomeController(IncomeService incomeService, UserService userService) {
        this.incomeService = incomeService;
        this.userService = userService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addIncome(@RequestBody IncomeRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        if (userService.findByUsername(request.getUsername()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "user does not exist"));
        }
        com.expensetracker.model.Income inc = new com.expensetracker.model.Income();
        inc.setUsername(request.getUsername());
        inc.setSource(request.getSource() == null ? "Salary" : request.getSource());
        inc.setAmount(request.getAmount());
        inc.setReceivedDate(request.getReceivedDate());
        inc.setMonth(request.getMonth());
        inc.setYear(request.getYear());
        com.expensetracker.model.Income saved = incomeService.addIncome(inc);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/range")
    public ResponseEntity<?> incomesByRange(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String fromMonthStr = body.get("fromMonth");
        String fromYearStr = body.get("fromYear");
        String toMonthStr = body.get("toMonth");
        String toYearStr = body.get("toYear");
        int page = body.get("page") != null ? Integer.parseInt(body.get("page")) : 0;
        int size = body.get("size") != null ? Integer.parseInt(body.get("size")) : 10;

        if (username == null || username.isBlank()
                || fromMonthStr == null || fromYearStr == null || toMonthStr == null || toYearStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username, fromMonth, fromYear, toMonth and toYear are required"));
        }

        if (!ALLOWED_PAGE_SIZES.contains(size)) return ResponseEntity.badRequest().body(Map.of("error", "invalid page size"));

        int fromMonth, fromYear, toMonth, toYear;
        try {
            fromMonth = Integer.parseInt(fromMonthStr);
            fromYear = Integer.parseInt(fromYearStr);
            toMonth = Integer.parseInt(toMonthStr);
            toYear = Integer.parseInt(toYearStr);
        } catch (NumberFormatException nfe) {
            return ResponseEntity.badRequest().body(Map.of("error", "month and year values must be integers"));
        }

        // validate month ranges
        if (fromMonth < 1 || fromMonth > 12 || toMonth < 1 || toMonth > 12) {
            return ResponseEntity.badRequest().body(Map.of("error", "month must be between 1 and 12"));
        }

        // build start and end dates: start = first day of fromMonth/fromYear, end = last day of toMonth/toYear
        try {
            LocalDate start = LocalDate.of(fromYear, fromMonth, 1);
            YearMonth ym = YearMonth.of(toYear, toMonth);
            LocalDate end = ym.atEndOfMonth();
            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().body(Map.of("error", "from date must be before or equal to to date"));
            }
            var pageResp = incomeService.getByUserAndDateRange(username, start, end, page, size);
            return ResponseEntity.ok(Map.of(
                    "content", pageResp.getContent(),
                    "page", pageResp.getNumber(),
                    "size", pageResp.getSize(),
                    "totalPages", pageResp.getTotalPages(),
                    "totalElements", pageResp.getTotalElements()
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid month/year combination"));
        }
    }

    @PostMapping("/month")
    public ResponseEntity<?> incomesForMonth(@RequestBody Map<String, Object> body) {
        if (body == null) return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        String username = (String) body.get("username");
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
            return ResponseEntity.badRequest().body(Map.of("error", "month and year must be integers"));
        }
        if (username == null || username.isBlank() || month == null || year == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username, month and year are required"));
        }
        if (month < 1 || month > 12) return ResponseEntity.badRequest().body(Map.of("error", "month must be between 1 and 12"));
        if (!ALLOWED_PAGE_SIZES.contains(size)) return ResponseEntity.badRequest().body(Map.of("error", "invalid page size"));
        try {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            var pageResp = incomeService.getByUserAndDateRange(username, start, end, page, size);
            return ResponseEntity.ok(Map.of(
                    "content", pageResp.getContent(),
                    "page", pageResp.getNumber(),
                    "size", pageResp.getSize(),
                    "totalPages", pageResp.getTotalPages(),
                    "totalElements", pageResp.getTotalElements()
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid month/year"));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteIncome(@RequestBody IncomeDeleteRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username is required"));
        }
        if (request.getIncomeId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "incomeId is required"));
        }
        boolean deleted = incomeService.deleteIncome(request.getUsername(), request.getIncomeId());
        if (!deleted) {
            return ResponseEntity.status(404).body(Map.of("error", "income not found or does not belong to user"));
        }
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @DeleteMapping("/{incomeId}")
    public ResponseEntity<?> deleteIncomeById(@PathVariable Integer incomeId) {
        incomeService.deleteIncome(incomeId);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateIncome(@RequestBody com.expensetracker.dto.IncomeUpdateRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        }
        if (request.getIncomeId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "incomeId required"));
        }
        // ensure user exists
        if (userService.findByUsername(request.getUsername()).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "user does not exist"));
        }
        com.expensetracker.model.Income upd = new com.expensetracker.model.Income();
        upd.setUsername(request.getUsername());
        upd.setSource(request.getSource());
        upd.setAmount(request.getAmount());
        upd.setReceivedDate(request.getReceivedDate());
        try {
            incomeService.updateIncome(request.getIncomeId(), request.getUsername(), upd);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }
}
