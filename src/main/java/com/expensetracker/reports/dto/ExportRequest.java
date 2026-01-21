package com.expensetracker.reports.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    private String userId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * Export type: EXPENSES, INCOME, or BOTH
     */
    private ExportType exportType = ExportType.BOTH;

    /**
     * Format: EXCEL or PDF
     */
    private ExportFormat format = ExportFormat.EXCEL;

    public enum ExportType {
        EXPENSES, INCOME, BOTH
    }

    public enum ExportFormat {
        EXCEL, PDF
    }
}
