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
@Table(name = "user_expense_category", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_expense_category_name"}))
public class UserExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_expense_category_id")
    private Integer userExpenseCategoryId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "user_expense_category_name", nullable = false, length = 100)
    private String userExpenseCategoryName;

    @Column(name = "last_update_tmstp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdateTmstp;

    @Column(name = "status", nullable = false, length = 1)
    private String status; // 'A' = active

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
