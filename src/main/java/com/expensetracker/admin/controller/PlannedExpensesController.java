package com.expensetracker.admin.controller;
import com.expensetracker.admin.model.PlannedExpenses;
import com.expensetracker.admin.service.PlannedExpensesService;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/admin/planned-expenses")
public class PlannedExpensesController {
    private static final Logger logger = LoggerFactory.getLogger(PlannedExpensesController.class);
    private final PlannedExpensesService plannedExpensesService;
    @Autowired
    public PlannedExpensesController(@Qualifier("adminPlannedExpensesService") PlannedExpensesService plannedExpensesService) {
        this.plannedExpensesService = plannedExpensesService;
    }
    @GetMapping("")
    public ResponseEntity<List<PlannedExpenses>> list() {
        return ResponseEntity.ok(plannedExpensesService.findAll());
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Integer id) {
        var opt = plannedExpensesService.findById(id);
        if (opt.isEmpty()) throw new ResourceNotFoundException("PlannedExpenses", "id", id);
        return ResponseEntity.ok(opt.get());
    }
    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody PlannedExpenses p) {
        if (p.getExpenseName() == null || p.getExpenseName().isBlank()) throw new BadRequestException("expenseName is required");
        if (p.getExpenseAmount() != null && p.getExpenseAmount() < 0) throw new BadRequestException("expenseAmount cannot be negative");
        return ResponseEntity.ok(plannedExpensesService.save(p));
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody PlannedExpenses p) {
        if (p.getExpenseAmount() != null && p.getExpenseAmount() < 0) throw new BadRequestException("expenseAmount cannot be negative");
        p.setPlannedExpensesId(id);
        return ResponseEntity.ok(plannedExpensesService.save(p));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        plannedExpensesService.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
    @PostMapping("/{userId}/copyMaster")
    public ResponseEntity<?> copyMaster(@PathVariable String userId) {
        int count = plannedExpensesService.copyPlannedToUser(userId);
        return ResponseEntity.ok(Map.of("status", "success", "inserted", count));
    }
}
