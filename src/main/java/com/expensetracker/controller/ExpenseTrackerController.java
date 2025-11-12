package com.expensetracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExpenseTrackerController {

    // Minimal health endpoint retained for backward-compatibility; all functionality
    // has been moved into finer-grained controllers: ExpenseController, IncomeController,
    // ExpenseCategoryController and UserController.
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
