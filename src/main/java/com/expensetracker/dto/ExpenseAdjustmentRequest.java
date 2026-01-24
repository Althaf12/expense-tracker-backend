package com.expensetracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseAdjustmentRequest {

    private Integer expenseAdjustmentsId; // optional for updates
    private Integer expensesId;
    private String userId;
    private String adjustmentType; // REFUND | CASHBACK | REVERSAL
    private BigDecimal adjustmentAmount;
    private String adjustmentReason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate adjustmentDate;

    private String status; // PENDING | COMPLETED | FAILED | CANCELLED
}
