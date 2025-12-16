package com.expensetracker.controller;

import com.expensetracker.model.PlannedExpenses;
import com.expensetracker.service.PlannedExpensesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planned-expenses")
public class PlannedExpensesController {

    private final PlannedExpensesService plannedExpensesService;

    @Autowired
    public PlannedExpensesController(PlannedExpensesService plannedExpensesService) {
        this.plannedExpensesService = plannedExpensesService;
    }

    @GetMapping("")
    public ResponseEntity<List<PlannedExpenses>> list() {
        return ResponseEntity.ok(plannedExpensesService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Integer id) {
        var opt = plannedExpensesService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(opt.get());
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody PlannedExpenses p) {
        return ResponseEntity.ok(plannedExpensesService.save(p));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody PlannedExpenses p) {
        p.setPlannedExpensesId(id);
        return ResponseEntity.ok(plannedExpensesService.save(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        plannedExpensesService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{username}/copyMaster")
    public ResponseEntity<?> copyMaster(@PathVariable String username) {
        int count = plannedExpensesService.copyPlannedToUser(username);
        return ResponseEntity.ok(Map.of("status", "success", "inserted", count));
    }
}
