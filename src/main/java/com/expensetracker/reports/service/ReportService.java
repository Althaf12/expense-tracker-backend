package com.expensetracker.reports.service;

import com.expensetracker.exception.BadRequestException;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseAdjustment;
import com.expensetracker.model.Income;
import com.expensetracker.model.UserExpenseCategory;
import com.expensetracker.reports.dto.EmailExportRequest;
import com.expensetracker.reports.dto.ExportRequest;
import com.expensetracker.reports.dto.ExportResponse;
import com.expensetracker.repository.ExpenseAdjustmentRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.IncomeRepository;
import com.expensetracker.repository.UserExpenseCategoryRepository;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long MAX_DATE_RANGE_DAYS = 366; // Max 1 year (including leap year)

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserExpenseCategoryRepository userExpenseCategoryRepository;
    private final ExpenseAdjustmentRepository adjustmentRepository;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final ReportEmailService reportEmailService;

    public ReportService(ExpenseRepository expenseRepository,
                         IncomeRepository incomeRepository,
                         UserExpenseCategoryRepository userExpenseCategoryRepository,
                         ExpenseAdjustmentRepository adjustmentRepository,
                         ExcelExportService excelExportService,
                         PdfExportService pdfExportService,
                         ReportEmailService reportEmailService) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.userExpenseCategoryRepository = userExpenseCategoryRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.excelExportService = excelExportService;
        this.pdfExportService = pdfExportService;
        this.reportEmailService = reportEmailService;
    }

    /**
     * Validate export request
     */
    public void validateRequest(String userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("User ID is required");
        }
        if (startDate == null) {
            throw new BadRequestException("Start date is required");
        }
        if (endDate == null) {
            throw new BadRequestException("End date is required");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > MAX_DATE_RANGE_DAYS) {
            throw new BadRequestException("Date range cannot exceed 1 year (366 days). Current range: " + daysBetween + " days");
        }
    }

    /**
     * Generate report based on format (Excel or PDF)
     */
    public byte[] generateReport(ExportRequest request) throws IOException {
        validateRequest(request.getUserId(), request.getStartDate(), request.getEndDate());

        logger.info("Generating {} report for user: {}, type: {}, date range: {} to {}",
                request.getFormat(), request.getUserId(), request.getExportType(),
                request.getStartDate(), request.getEndDate());

        // Fetch data
        List<Expense> expenses = Collections.emptyList();
        List<Income> incomes = Collections.emptyList();
        Map<Integer, String> categoryMap = Collections.emptyMap();
        Map<Integer, BigDecimal> adjustmentsMap = Collections.emptyMap();

        if (request.getExportType() == ExportRequest.ExportType.EXPENSES ||
                request.getExportType() == ExportRequest.ExportType.BOTH) {
            expenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                    request.getUserId(), request.getStartDate(), request.getEndDate());
            categoryMap = getCategoryMap(request.getUserId());

            // Fetch adjustments for all expenses
            if (!expenses.isEmpty()) {
                List<Integer> expenseIds = expenses.stream()
                        .map(Expense::getExpensesId)
                        .collect(Collectors.toList());
                adjustmentsMap = getAdjustmentsMap(expenseIds);
            }
        }

        if (request.getExportType() == ExportRequest.ExportType.INCOME ||
                request.getExportType() == ExportRequest.ExportType.BOTH) {
            incomes = incomeRepository.findByUserIdAndReceivedDateBetween(
                    request.getUserId(), request.getStartDate(), request.getEndDate());
        }

        // Generate report based on format
        if (request.getFormat() == ExportRequest.ExportFormat.EXCEL) {
            return excelExportService.generateReport(expenses, incomes, categoryMap, adjustmentsMap,
                    request.getExportType(), request.getStartDate(), request.getEndDate());
        } else {
            return pdfExportService.generateReport(expenses, incomes, categoryMap, adjustmentsMap,
                    request.getExportType(), request.getStartDate(), request.getEndDate());
        }
    }

    /**
     * Get a map of expense ID to total completed adjustment amount
     */
    private Map<Integer, BigDecimal> getAdjustmentsMap(List<Integer> expenseIds) {
        if (expenseIds == null || expenseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ExpenseAdjustment> completedAdjustments = adjustmentRepository.findCompletedAdjustmentsForExpenses(expenseIds);
        return completedAdjustments.stream()
                .collect(Collectors.groupingBy(
                        ExpenseAdjustment::getExpensesId,
                        Collectors.reducing(BigDecimal.ZERO,
                                ExpenseAdjustment::getAdjustmentAmount,
                                BigDecimal::add)
                ));
    }

    /**
     * Generate and email report
     */
    public ExportResponse generateAndEmailReport(EmailExportRequest request) {
        validateRequest(request.getUserId(), request.getStartDate(), request.getEndDate());

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email address is required");
        }

        if (!isValidEmail(request.getEmail())) {
            throw new BadRequestException("Invalid email address format");
        }

        if (!reportEmailService.isMailServiceAvailable()) {
            throw new BadRequestException("Email service is not configured. Please check mail server settings.");
        }

        logger.info("Generating and emailing {} report to: {}, type: {}, date range: {} to {}",
                request.getFormat(), request.getEmail(), request.getExportType(),
                request.getStartDate(), request.getEndDate());

        try {
            // Convert to ExportRequest
            ExportRequest exportRequest = new ExportRequest();
            exportRequest.setUserId(request.getUserId());
            exportRequest.setStartDate(request.getStartDate());
            exportRequest.setEndDate(request.getEndDate());
            exportRequest.setExportType(request.getExportType());
            exportRequest.setFormat(request.getFormat());

            // Generate report
            byte[] reportData = generateReport(exportRequest);

            // Generate file name
            String fileName = generateFileName(request);

            // Get record count
            int totalRecords = getTotalRecords(request);

            // Send email
            String reportTypeName = getReportTypeName(request.getExportType());
            int emailsSent = reportEmailService.sendReport(
                    request.getEmail(),
                    reportData,
                    fileName,
                    reportTypeName,
                    request.getStartDate(),
                    request.getEndDate()
            );

            String message = emailsSent == 1 ?
                    "Report sent successfully to " + request.getEmail() :
                    "Report sent successfully in " + emailsSent + " parts to " + request.getEmail();

            return ExportResponse.success(message, fileName, totalRecords);

        } catch (IOException e) {
            logger.error("Error generating report", e);
            return ExportResponse.error("Failed to generate report: " + e.getMessage());
        } catch (MessagingException e) {
            logger.error("Error sending email", e);
            return ExportResponse.error("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Get total number of records for the report
     */
    public int getTotalRecords(ExportRequest request) {
        int count = 0;
        if (request.getExportType() == ExportRequest.ExportType.EXPENSES ||
                request.getExportType() == ExportRequest.ExportType.BOTH) {
            count += expenseRepository.findByUserIdAndExpenseDateBetween(
                    request.getUserId(), request.getStartDate(), request.getEndDate()).size();
        }
        if (request.getExportType() == ExportRequest.ExportType.INCOME ||
                request.getExportType() == ExportRequest.ExportType.BOTH) {
            count += incomeRepository.findByUserIdAndReceivedDateBetween(
                    request.getUserId(), request.getStartDate(), request.getEndDate()).size();
        }
        return count;
    }

    private int getTotalRecords(EmailExportRequest request) {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setUserId(request.getUserId());
        exportRequest.setStartDate(request.getStartDate());
        exportRequest.setEndDate(request.getEndDate());
        exportRequest.setExportType(request.getExportType());
        return getTotalRecords(exportRequest);
    }

    /**
     * Generate file name for the report
     */
    public String generateFileName(ExportRequest request) {
        String typeName = switch (request.getExportType()) {
            case EXPENSES -> "expenses";
            case INCOME -> "income";
            case BOTH -> "financial_report";
        };

        String extension = request.getFormat() == ExportRequest.ExportFormat.EXCEL ? ".xlsx" : ".pdf";

        return String.format("%s_%s_%s%s",
                typeName,
                request.getStartDate().format(FILE_DATE_FORMATTER),
                request.getEndDate().format(FILE_DATE_FORMATTER),
                extension);
    }

    private String generateFileName(EmailExportRequest request) {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setStartDate(request.getStartDate());
        exportRequest.setEndDate(request.getEndDate());
        exportRequest.setExportType(request.getExportType());
        exportRequest.setFormat(request.getFormat());
        return generateFileName(exportRequest);
    }

    private Map<Integer, String> getCategoryMap(String userId) {
        List<UserExpenseCategory> categories = userExpenseCategoryRepository
                .findByUserIdOrderByUserExpenseCategoryName(userId);
        return categories.stream()
                .collect(Collectors.toMap(
                        UserExpenseCategory::getUserExpenseCategoryId,
                        UserExpenseCategory::getUserExpenseCategoryName,
                        (existing, replacement) -> existing
                ));
    }

    private String getReportTypeName(ExportRequest.ExportType exportType) {
        return switch (exportType) {
            case EXPENSES -> "Expenses";
            case INCOME -> "Income";
            case BOTH -> "Expenses & Income";
        };
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        // Basic email validation
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
