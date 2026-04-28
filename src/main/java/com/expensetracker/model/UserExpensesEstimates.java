package com.expensetracker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_expenses_estimates")
public class UserExpensesEstimates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_expenses_estimates_id")
    private Integer userExpensesEstimatesId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "user_expense_name", length = 100)
    private String userExpenseName;

    @Column(name = "user_expense_category_id", nullable = false)
    private Integer userExpenseCategoryId;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "last_update_tmstp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdateTmstp;

    @Column(name = "status", nullable = false, length = 1)
    private String status; // 'A' = active, 'I' = inactive

    @PrePersist
    public void prePersist() {
        if (this.lastUpdateTmstp == null) {
            this.lastUpdateTmstp = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "A";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdateTmstp = LocalDateTime.now();
    }
}

