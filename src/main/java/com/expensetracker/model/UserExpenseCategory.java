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
       uniqueConstraints = @UniqueConstraint(columnNames = {"username", "user_expense_category_name"}))
public class UserExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_expense_category_id")
    private Integer userExpenseCategoryId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "user_expense_category_name", nullable = false)
    private String userExpenseCategoryName;

    @Column(name = "last_update_tmstp", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdateTmstp;

    @Column(name = "status", nullable = false, length = 1)
    private String status; // 'A' = active
}
