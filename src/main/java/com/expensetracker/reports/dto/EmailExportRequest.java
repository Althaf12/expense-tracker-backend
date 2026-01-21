package com.expensetracker.reports.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailExportRequest {

    private String userId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * Export type: EXPENSES, INCOME, or BOTH
     */
    private ExportRequest.ExportType exportType = ExportRequest.ExportType.BOTH;

    /**
     * Format: EXCEL or PDF
     */
    private ExportRequest.ExportFormat format = ExportRequest.ExportFormat.EXCEL;

    /**
     * Email address to send the report
     */
    private String email;
}
