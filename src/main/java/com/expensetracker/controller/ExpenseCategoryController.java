package com.expensetracker.controller;

import com.expensetracker.dto.ExpenseCategoryRequest;
import com.expensetracker.service.ExpenseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/expense-category")
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;

    @Autowired
    public ExpenseCategoryController(ExpenseCategoryService expenseCategoryService) {
        this.expenseCategoryService = expenseCategoryService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addExpenseCategory(@RequestBody ExpenseCategoryRequest request) {
        if (request == null || request.getExpenseCategoryName() == null || request.getExpenseCategoryName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "expenseCategoryName required"));
        }
        com.expensetracker.model.ExpenseCategory c = new com.expensetracker.model.ExpenseCategory();
        c.setExpenseCategoryName(request.getExpenseCategoryName());
        com.expensetracker.model.ExpenseCategory saved = expenseCategoryService.addOrUpdate(c);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllCategories() {
        List<com.expensetracker.model.ExpenseCategory> list = expenseCategoryService.findAll();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpenseCategory(@PathVariable Integer id) {
        expenseCategoryService.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateExpenseCategory(@RequestBody Map<String, String> body) {
        String idStr = body.get("expenseCategoryId");
        String name = body.get("expenseCategoryName");
        String newName = body.get("newName");
        if ((idStr == null || idStr.isBlank()) && (name == null || name.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "provide expenseCategoryId or expenseCategoryName to identify category"));
        }
        if (newName == null || newName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "newName required"));
        }
        try {
            if (idStr != null && !idStr.isBlank()) {
                Integer id = Integer.parseInt(idStr);
                expenseCategoryService.updateById(id, newName);
                return ResponseEntity.ok(Map.of("status", "success"));
            } else {
                expenseCategoryService.updateByName(name, newName);
                return ResponseEntity.ok(Map.of("status", "success"));
            }
        } catch (NumberFormatException nfe) {
            return ResponseEntity.badRequest().body(Map.of("error", "expenseCategoryId must be integer"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }
}
