package com.expensetracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expenses_id")
    private Integer expensesId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "expense_name")
    private String expenseName;

    @Column(name = "expense_amount")
    private Double expenseAmount;

    @Column(name = "expense_category_id")
    private Integer expenseCategoryId;

    // let DB have default but also set from application when inserting/updating
    @Column(name = "last_update_tmstp", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastUpdateTmstp;

    // new column
    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @PrePersist
    public void prePersist() {
        if (this.lastUpdateTmstp == null) {
            this.lastUpdateTmstp = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdateTmstp = LocalDateTime.now();
    }

}
