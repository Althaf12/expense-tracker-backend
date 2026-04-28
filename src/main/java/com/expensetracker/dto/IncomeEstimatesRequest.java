package com.expensetracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class IncomeEstimatesRequest {

    private String userId;

    private String source;  // defaults to "Salary" if not provided

    private BigDecimal amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate receivedDate;

    private String month;   // e.g. "April"

    private Integer year;   // e.g. 2026
}

