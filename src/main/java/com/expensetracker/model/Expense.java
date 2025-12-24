package com.expensetracker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "expense_name", length = 100)
    private String expenseName;

    @Column(name = "expense_amount")
    private Double expenseAmount;

    @Column(name = "user_expense_category_id", nullable = false)
    private Integer userExpenseCategoryId;

    @Column(name = "last_update_tmstp", columnDefinition = "TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdateTmstp;

    @Column(name = "expense_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
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
