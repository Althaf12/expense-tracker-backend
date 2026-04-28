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
@Table(name = "user_credit_card_estimates")
public class UserCreditCardEstimates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_credit_card_estimates_id")
    private Integer userCreditCardEstimatesId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "card_name", nullable = false, length = 100)
    private String cardName;

    @Column(name = "expense_name", length = 100)
    private String expenseName;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "last_update_tmstp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdateTmstp;

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

