package com.expensetracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expense_category")
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_category_id")
    private Integer expenseCategoryId;

    @Column(name = "expense_category_name")
    private String expenseCategoryName;

    @Column(name = "last_update_tmstp")
    private LocalDateTime lastUpdateTmstp;
}

