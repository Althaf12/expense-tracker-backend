package com.expensetracker.reports.service;

import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.reports.dto.ExportRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Generate Excel report for expenses and/or income
     */
    public byte[] generateReport(List<Expense> expenses, List<Income> incomes,
                                  Map<Integer, String> categoryMap,
                                  ExportRequest.ExportType exportType,
                                  LocalDate startDate, LocalDate endDate) throws IOException {

        logger.info("Generating Excel report for type: {}, date range: {} to {}", exportType, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Create cell styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            // Create Expenses sheet if needed
            if (exportType == ExportRequest.ExportType.EXPENSES || exportType == ExportRequest.ExportType.BOTH) {
                createExpensesSheet(workbook, expenses, categoryMap, headerStyle, dateStyle, currencyStyle, titleStyle, startDate, endDate);
            }

            // Create Income sheet if needed
            if (exportType == ExportRequest.ExportType.INCOME || exportType == ExportRequest.ExportType.BOTH) {
                createIncomeSheet(workbook, incomes, headerStyle, dateStyle, currencyStyle, titleStyle, startDate, endDate);
            }

            // Create Summary sheet if both
            if (exportType == ExportRequest.ExportType.BOTH) {
                createSummarySheet(workbook, expenses, incomes, headerStyle, currencyStyle, titleStyle, startDate, endDate);
            }

            workbook.write(outputStream);
            logger.info("Excel report generated successfully");
            return outputStream.toByteArray();
        }
    }

    private void createExpensesSheet(Workbook workbook, List<Expense> expenses,
                                      Map<Integer, String> categoryMap,
                                      CellStyle headerStyle, CellStyle dateStyle,
                                      CellStyle currencyStyle, CellStyle titleStyle,
                                      LocalDate startDate, LocalDate endDate) {

        Sheet sheet = workbook.createSheet("Expenses");

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Expenses Report");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        // Date range
        Row dateRangeRow = sheet.createRow(rowNum++);
        dateRangeRow.createCell(0).setCellValue("Period: " + startDate.format(DATE_FORMATTER) + " to " + endDate.format(DATE_FORMATTER));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

        rowNum++; // Empty row

        // Header row
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Date", "Expense Name", "Category", "Amount", "Last Updated"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        double totalAmount = 0;
        for (Expense expense : expenses) {
            Row row = sheet.createRow(rowNum++);

            Cell dateCell = row.createCell(0);
            if (expense.getExpenseDate() != null) {
                dateCell.setCellValue(expense.getExpenseDate().format(DATE_FORMATTER));
            }
            dateCell.setCellStyle(dateStyle);

            row.createCell(1).setCellValue(expense.getExpenseName() != null ? expense.getExpenseName() : "");

            String categoryName = categoryMap.getOrDefault(expense.getUserExpenseCategoryId(), "Unknown");
            row.createCell(2).setCellValue(categoryName);

            Cell amountCell = row.createCell(3);
            if (expense.getExpenseAmount() != null) {
                amountCell.setCellValue(expense.getExpenseAmount());
                totalAmount += expense.getExpenseAmount();
            }
            amountCell.setCellStyle(currencyStyle);

            Cell lastUpdatedCell = row.createCell(4);
            if (expense.getLastUpdateTmstp() != null) {
                lastUpdatedCell.setCellValue(expense.getLastUpdateTmstp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
        }

        // Total row
        rowNum++; // Empty row
        Row totalRow = sheet.createRow(rowNum);
        totalRow.createCell(2).setCellValue("Total:");
        Cell totalCell = totalRow.createCell(3);
        totalCell.setCellValue(totalAmount);
        totalCell.setCellStyle(currencyStyle);

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createIncomeSheet(Workbook workbook, List<Income> incomes,
                                    CellStyle headerStyle, CellStyle dateStyle,
                                    CellStyle currencyStyle, CellStyle titleStyle,
                                    LocalDate startDate, LocalDate endDate) {

        Sheet sheet = workbook.createSheet("Income");

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Income Report");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        // Date range
        Row dateRangeRow = sheet.createRow(rowNum++);
        dateRangeRow.createCell(0).setCellValue("Period: " + startDate.format(DATE_FORMATTER) + " to " + endDate.format(DATE_FORMATTER));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

        rowNum++; // Empty row

        // Header row
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Date", "Source", "Amount", "Month", "Year"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        double totalAmount = 0;
        for (Income income : incomes) {
            Row row = sheet.createRow(rowNum++);

            Cell dateCell = row.createCell(0);
            if (income.getReceivedDate() != null) {
                dateCell.setCellValue(income.getReceivedDate().format(DATE_FORMATTER));
            }
            dateCell.setCellStyle(dateStyle);

            row.createCell(1).setCellValue(income.getSource() != null ? income.getSource() : "");

            Cell amountCell = row.createCell(2);
            if (income.getAmount() != null) {
                amountCell.setCellValue(income.getAmount());
                totalAmount += income.getAmount();
            }
            amountCell.setCellStyle(currencyStyle);

            row.createCell(3).setCellValue(income.getMonth() != null ? income.getMonth() : "");
            row.createCell(4).setCellValue(income.getYear() != null ? income.getYear() : 0);
        }

        // Total row
        rowNum++; // Empty row
        Row totalRow = sheet.createRow(rowNum);
        totalRow.createCell(1).setCellValue("Total:");
        Cell totalCell = totalRow.createCell(2);
        totalCell.setCellValue(totalAmount);
        totalCell.setCellStyle(currencyStyle);

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createSummarySheet(Workbook workbook, List<Expense> expenses, List<Income> incomes,
                                     CellStyle headerStyle, CellStyle currencyStyle, CellStyle titleStyle,
                                     LocalDate startDate, LocalDate endDate) {

        Sheet sheet = workbook.createSheet("Summary");

        double totalExpenses = expenses.stream()
                .filter(e -> e.getExpenseAmount() != null)
                .mapToDouble(Expense::getExpenseAmount)
                .sum();

        double totalIncome = incomes.stream()
                .filter(i -> i.getAmount() != null)
                .mapToDouble(Income::getAmount)
                .sum();

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Financial Summary");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // Date range
        Row dateRangeRow = sheet.createRow(rowNum++);
        dateRangeRow.createCell(0).setCellValue("Period: " + startDate.format(DATE_FORMATTER) + " to " + endDate.format(DATE_FORMATTER));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));

        rowNum++; // Empty row

        // Summary data
        Row incomeRow = sheet.createRow(rowNum++);
        incomeRow.createCell(0).setCellValue("Total Income:");
        Cell incomeTotalCell = incomeRow.createCell(1);
        incomeTotalCell.setCellValue(totalIncome);
        incomeTotalCell.setCellStyle(currencyStyle);

        Row expenseRow = sheet.createRow(rowNum++);
        expenseRow.createCell(0).setCellValue("Total Expenses:");
        Cell expenseTotalCell = expenseRow.createCell(1);
        expenseTotalCell.setCellValue(totalExpenses);
        expenseTotalCell.setCellStyle(currencyStyle);

        rowNum++; // Empty row

        Row balanceRow = sheet.createRow(rowNum++);
        balanceRow.createCell(0).setCellValue("Net Balance:");
        Cell balanceCell = balanceRow.createCell(1);
        balanceCell.setCellValue(totalIncome - totalExpenses);
        balanceCell.setCellStyle(currencyStyle);

        rowNum++; // Empty row

        Row countRow = sheet.createRow(rowNum++);
        countRow.createCell(0).setCellValue("Number of Expense Records:");
        countRow.createCell(1).setCellValue(expenses.size());

        Row incomeCountRow = sheet.createRow(rowNum);
        incomeCountRow.createCell(0).setCellValue("Number of Income Records:");
        incomeCountRow.createCell(1).setCellValue(incomes.size());

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }
}
