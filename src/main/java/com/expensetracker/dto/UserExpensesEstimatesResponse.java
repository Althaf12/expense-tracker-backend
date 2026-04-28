package com.expensetracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserExpensesEstimatesResponse {
    private Integer userExpensesEstimatesId;
    private String userId;
    private String userExpenseName;
    private Integer userExpenseCategoryId;
    private String userExpenseCategoryName;
    private BigDecimal amount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdateTmstp;
    private String status;
}

