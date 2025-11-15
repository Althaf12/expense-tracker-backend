package com.expensetracker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_expenses")
public class UserExpenses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_expenses_id")
    private Integer userExpensesId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "user_expense_name", nullable = false)
    private String userExpenseName;

    @Column(name = "user_expense_category_id", nullable = false)
    private Integer userExpenseCategoryId;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "paid")
    private String paid;

    @Column(name = "last_update_tmstp", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdateTmstp;

    @Column(name = "status", nullable = false, length = 1)
    private String status; // 'A' = active

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
