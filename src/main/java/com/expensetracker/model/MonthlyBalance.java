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
@Table(name = "monthly_balance")
public class MonthlyBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_balance_id")
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "year")
    private Integer year;

    @Column(name = "month")
    private Integer month;

    @Column(name = "opening_balance", precision = 10, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", precision = 10, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "created_tmstp", columnDefinition = "TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdTmstp;

    @PrePersist
    public void prePersist() {
        if (this.createdTmstp == null) {
            this.createdTmstp = LocalDateTime.now();
        }
    }
}
