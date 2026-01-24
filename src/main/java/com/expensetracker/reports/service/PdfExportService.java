package com.expensetracker.reports.service;

import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.reports.dto.ExportRequest;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfExportService {

    private static final Logger logger = LoggerFactory.getLogger(PdfExportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);

    /**
     * Generate PDF report for expenses and/or income
     */
    public byte[] generateReport(List<Expense> expenses, List<Income> incomes,
                                  Map<Integer, String> categoryMap,
                                  ExportRequest.ExportType exportType,
                                  LocalDate startDate, LocalDate endDate) throws IOException {

        logger.info("Generating PDF report for type: {}, date range: {} to {}", exportType, startDate, endDate);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Add metadata
            document.addTitle("Expense Tracker Report");
            document.addSubject("Financial Report");
            document.addCreator("Expense Tracker Application");

            // Add main title
            Paragraph mainTitle = new Paragraph("Expense Tracker Report", TITLE_FONT);
            mainTitle.setAlignment(Element.ALIGN_CENTER);
            mainTitle.setSpacingAfter(10);
            document.add(mainTitle);

            // Add date range
            Paragraph dateRange = new Paragraph(
                    "Period: " + startDate.format(DATE_FORMATTER) + " to " + endDate.format(DATE_FORMATTER),
                    NORMAL_FONT);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(20);
            document.add(dateRange);

            // Add summary if BOTH
            if (exportType == ExportRequest.ExportType.BOTH) {
                addSummarySection(document, expenses, incomes);
            }

            // Add Expenses section if needed
            if (exportType == ExportRequest.ExportType.EXPENSES || exportType == ExportRequest.ExportType.BOTH) {
                addExpensesSection(document, expenses, categoryMap);
            }

            // Add Income section if needed
            if (exportType == ExportRequest.ExportType.INCOME || exportType == ExportRequest.ExportType.BOTH) {
                addIncomeSection(document, incomes);
            }

            document.close();
            logger.info("PDF report generated successfully");
            return outputStream.toByteArray();
        } catch (DocumentException e) {
            logger.error("Error generating PDF report", e);
            throw new IOException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    private void addSummarySection(Document document, List<Expense> expenses, List<Income> incomes)
            throws DocumentException {

        double totalExpenses = expenses.stream()
                .filter(e -> e.getExpenseAmount() != null)
                .mapToDouble(e -> e.getExpenseAmount().doubleValue())
                .sum();

        double totalIncome = incomes.stream()
                .filter(i -> i.getAmount() != null)
                .mapToDouble(i -> i.getAmount().doubleValue())
                .sum();

        Paragraph sectionTitle = new Paragraph("Financial Summary", BOLD_FONT);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(50);
        summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        summaryTable.setWidths(new float[]{2, 1});

        addSummaryRow(summaryTable, "Total Income:", String.format("%.2f", totalIncome));
        addSummaryRow(summaryTable, "Total Expenses:", String.format("%.2f", totalExpenses));
        addSummaryRow(summaryTable, "Net Balance:", String.format("%.2f", totalIncome - totalExpenses));
        addSummaryRow(summaryTable, "Expense Records:", String.valueOf(expenses.size()));
        addSummaryRow(summaryTable, "Income Records:", String.valueOf(incomes.size()));

        document.add(summaryTable);
        document.add(new Paragraph(" ")); // Spacing
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addExpensesSection(Document document, List<Expense> expenses, Map<Integer, String> categoryMap)
            throws DocumentException {

        Paragraph sectionTitle = new Paragraph("Expenses", BOLD_FONT);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        if (expenses.isEmpty()) {
            document.add(new Paragraph("No expense records found for the selected period.", NORMAL_FONT));
            return;
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 2.5f, 2f, 1.5f});

        // Add headers
        addTableHeader(table, "Date");
        addTableHeader(table, "Expense Name");
        addTableHeader(table, "Category");
        addTableHeader(table, "Amount");

        // Add data rows
        double total = 0;
        for (Expense expense : expenses) {
            addTableCell(table, expense.getExpenseDate() != null ?
                    expense.getExpenseDate().format(DATE_FORMATTER) : "");
            addTableCell(table, expense.getExpenseName() != null ? expense.getExpenseName() : "");
            addTableCell(table, categoryMap.getOrDefault(expense.getUserExpenseCategoryId(), "Unknown"));

            String amount = expense.getExpenseAmount() != null ?
                    String.format("%.2f", expense.getExpenseAmount().doubleValue()) : "0.00";
            addTableCellRight(table, amount);

            if (expense.getExpenseAmount() != null) {
                total += expense.getExpenseAmount().doubleValue();
            }
        }

        document.add(table);

        // Add total
        Paragraph totalPara = new Paragraph(String.format("Total Expenses: %.2f", total), BOLD_FONT);
        totalPara.setAlignment(Element.ALIGN_RIGHT);
        totalPara.setSpacingBefore(5);
        document.add(totalPara);
    }

    private void addIncomeSection(Document document, List<Income> incomes) throws DocumentException {

        Paragraph sectionTitle = new Paragraph("Income", BOLD_FONT);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        if (incomes.isEmpty()) {
            document.add(new Paragraph("No income records found for the selected period.", NORMAL_FONT));
            return;
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 2.5f, 1.5f, 1.5f});

        // Add headers
        addTableHeader(table, "Date");
        addTableHeader(table, "Source");
        addTableHeader(table, "Month/Year");
        addTableHeader(table, "Amount");

        // Add data rows
        double total = 0;
        for (Income income : incomes) {
            addTableCell(table, income.getReceivedDate() != null ?
                    income.getReceivedDate().format(DATE_FORMATTER) : "");
            addTableCell(table, income.getSource() != null ? income.getSource() : "");
            addTableCell(table, (income.getMonth() != null ? income.getMonth() : "") +
                    " " + (income.getYear() != null ? income.getYear() : ""));

            String amount = income.getAmount() != null ?
                    String.format("%.2f", income.getAmount().doubleValue()) : "0.00";
            addTableCellRight(table, amount);

            if (income.getAmount() != null) {
                total += income.getAmount().doubleValue();
            }
        }

        document.add(table);

        // Add total
        Paragraph totalPara = new Paragraph(String.format("Total Income: %.2f", total), BOLD_FONT);
        totalPara.setAlignment(Element.ALIGN_RIGHT);
        totalPara.setSpacingBefore(5);
        document.add(totalPara);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new Color(70, 130, 180)); // Steel blue
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addTableCellRight(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }
}
