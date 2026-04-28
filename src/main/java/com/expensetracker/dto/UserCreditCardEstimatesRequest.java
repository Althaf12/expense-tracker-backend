package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreditCardEstimatesRequest {
    private String cardName;
    private String expenseName;
    private BigDecimal amount;
}

