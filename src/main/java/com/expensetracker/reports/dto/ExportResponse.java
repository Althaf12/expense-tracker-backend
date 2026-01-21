package com.expensetracker.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportResponse {

    private boolean success;
    private String message;
    private String fileName;
    private int totalRecords;

    public static ExportResponse success(String message, String fileName, int totalRecords) {
        return new ExportResponse(true, message, fileName, totalRecords);
    }

    public static ExportResponse error(String message) {
        return new ExportResponse(false, message, null, 0);
    }
}
