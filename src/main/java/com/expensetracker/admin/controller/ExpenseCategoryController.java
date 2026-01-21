package com.expensetracker.admin.controller;
import com.expensetracker.admin.dto.ExpenseCategoryRequest;
import com.expensetracker.admin.service.ExpenseCategoryService;
import com.expensetracker.admin.model.ExpenseCategory;
import com.expensetracker.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
@RestController
@RequestMapping("/api/admin/expense-category")
public class ExpenseCategoryController {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseCategoryController.class);
    private final ExpenseCategoryService expenseCategoryService;
    @Autowired
    public ExpenseCategoryController(@Qualifier("adminExpenseCategoryService") ExpenseCategoryService expenseCategoryService) {
        this.expenseCategoryService = expenseCategoryService;
    }
    @PostMapping("/add")
    public ResponseEntity<?> addExpenseCategory(@RequestBody ExpenseCategoryRequest request) {
        logger.debug("addExpenseCategory called");
        if (request == null || request.getExpenseCategoryName() == null || request.getExpenseCategoryName().isBlank())
            throw new BadRequestException("expenseCategoryName is required");
        ExpenseCategory c = new ExpenseCategory();
        c.setExpenseCategoryName(request.getExpenseCategoryName());
        expenseCategoryService.addOrUpdate(c);
        logger.info("Expense category added: {}", request.getExpenseCategoryName());
        return ResponseEntity.ok(Map.of("status", "success"));
    }
    @GetMapping("/all")
    public ResponseEntity<?> getAllCategories() {
        List<ExpenseCategory> list = expenseCategoryService.findAll();
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
        if ((idStr == null || idStr.isBlank()) && (name == null || name.isBlank()))
            throw new BadRequestException("Provide expenseCategoryId or expenseCategoryName");
        if (newName == null || newName.isBlank()) throw new BadRequestException("newName is required");
        if (idStr != null && !idStr.isBlank()) {
            Integer id = Integer.parseInt(idStr);
            expenseCategoryService.updateById(id, newName);
        } else {
            expenseCategoryService.updateByName(name, newName);
        }
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
