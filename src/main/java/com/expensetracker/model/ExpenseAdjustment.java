package com.expensetracker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expense_adjustments")
public class ExpenseAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_adjustments_id")
    private Integer expenseAdjustmentsId;

    @Column(name = "expenses_id", nullable = false)
    private Integer expensesId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "adjustment_type", nullable = false, length = 20)
    private String adjustmentType; // REFUND | CASHBACK | REVERSAL

    @Column(name = "adjustment_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal adjustmentAmount;

    @Column(name = "adjustment_reason", length = 100)
    private String adjustmentReason;

    @Column(name = "adjustment_date", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate adjustmentDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING | COMPLETED | FAILED | CANCELLED

    @Column(name = "created_at", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "last_update_tmstp", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdateTmstp;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.lastUpdateTmstp == null) {
            this.lastUpdateTmstp = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdateTmstp = LocalDateTime.now();
    }
}
