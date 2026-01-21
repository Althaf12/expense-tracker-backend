package com.expensetracker.reports.controller;

import com.expensetracker.reports.dto.EmailExportRequest;
import com.expensetracker.reports.dto.ExportRequest;
import com.expensetracker.reports.dto.ExportResponse;
import com.expensetracker.reports.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Export expenses report as Excel
     * GET /api/reports/expenses/excel?userId={userId}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/expenses/excel")
    public ResponseEntity<byte[]> exportExpensesExcel(
            @RequestParam String userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        logger.info("Export expenses Excel request - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);

        ExportRequest request = new ExportRequest();
        request.setUserId(userId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setExportType(ExportRequest.ExportType.EXPENSES);
        request.setFormat(ExportRequest.ExportFormat.EXCEL);

        return generateDownloadResponse(request);
    }

    /**
     * Export expenses report as PDF
     * GET /api/reports/expenses/pdf?userId={userId}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/expenses/pdf")
    public ResponseEntity<byte[]> exportExpensesPdf(
            @RequestParam String userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        logger.info("Export expenses PDF request - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);

        ExportRequest request = new ExportRequest();
        request.setUserId(userId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setExportType(ExportRequest.ExportType.EXPENSES);
        request.setFormat(ExportRequest.ExportFormat.PDF);

        return generateDownloadResponse(request);
    }

    /**
     * Export income report as Excel
     * GET /api/reports/income/excel?userId={userId}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/income/excel")
    public ResponseEntity<byte[]> exportIncomeExcel(
            @RequestParam String userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        logger.info("Export income Excel request - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);

        ExportRequest request = new ExportRequest();
        request.setUserId(userId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setExportType(ExportRequest.ExportType.INCOME);
        request.setFormat(ExportRequest.ExportFormat.EXCEL);

        return generateDownloadResponse(request);
    }

    /**
     * Export income report as PDF
     * GET /api/reports/income/pdf?userId={userId}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/income/pdf")
    public ResponseEntity<byte[]> exportIncomePdf(
            @RequestParam String userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        logger.info("Export income PDF request - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);

        ExportRequest request = new ExportRequest();
        request.setUserId(userId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setExportType(ExportRequest.ExportType.INCOME);
        request.setFormat(ExportRequest.ExportFormat.PDF);

        return generateDownloadResponse(request);
    }

    /**
     * Export both expenses and income as Excel
     * GET /api/reports/all/excel?userId={userId}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/all/excel")
    public ResponseEntity<byte[]> exportAllExcel(
            @RequestParam String userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        logger.info("Export all (expenses + income) Excel request - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);

        ExportRequest request = new ExportRequest();
        request.setUserId(userId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setExportType(ExportRequest.ExportType.BOTH);
        request.setFormat(ExportRequest.ExportFormat.EXCEL);

        return generateDownloadResponse(request);
    }

    /**
     * Export both expenses and income as PDF
     * GET /api/reports/all/pdf?userId={userId}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/all/pdf")
    public ResponseEntity<byte[]> exportAllPdf(
            @RequestParam String userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        logger.info("Export all (expenses + income) PDF request - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);

        ExportRequest request = new ExportRequest();
        request.setUserId(userId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setExportType(ExportRequest.ExportType.BOTH);
        request.setFormat(ExportRequest.ExportFormat.PDF);

        return generateDownloadResponse(request);
    }

    /**
     * Generic export endpoint using POST
     * POST /api/reports/export
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportReport(@RequestBody ExportRequest request) {
        logger.info("Generic export request - userId: {}, type: {}, format: {}, startDate: {}, endDate: {}",
                request.getUserId(), request.getExportType(), request.getFormat(),
                request.getStartDate(), request.getEndDate());

        return generateDownloadResponse(request);
    }

    /**
     * Email expenses report
     * POST /api/reports/expenses/email
     */
    @PostMapping("/expenses/email")
    public ResponseEntity<ExportResponse> emailExpensesReport(@RequestBody EmailExportRequest request) {
        logger.info("Email expenses report request - userId: {}, email: {}, startDate: {}, endDate: {}",
                request.getUserId(), request.getEmail(), request.getStartDate(), request.getEndDate());

        request.setExportType(ExportRequest.ExportType.EXPENSES);
        return emailReport(request);
    }

    /**
     * Email income report
     * POST /api/reports/income/email
     */
    @PostMapping("/income/email")
    public ResponseEntity<ExportResponse> emailIncomeReport(@RequestBody EmailExportRequest request) {
        logger.info("Email income report request - userId: {}, email: {}, startDate: {}, endDate: {}",
                request.getUserId(), request.getEmail(), request.getStartDate(), request.getEndDate());

        request.setExportType(ExportRequest.ExportType.INCOME);
        return emailReport(request);
    }

    /**
     * Email both expenses and income report
     * POST /api/reports/all/email
     */
    @PostMapping("/all/email")
    public ResponseEntity<ExportResponse> emailAllReport(@RequestBody EmailExportRequest request) {
        logger.info("Email all (expenses + income) report request - userId: {}, email: {}, startDate: {}, endDate: {}",
                request.getUserId(), request.getEmail(), request.getStartDate(), request.getEndDate());

        request.setExportType(ExportRequest.ExportType.BOTH);
        return emailReport(request);
    }

    /**
     * Generic email endpoint
     * POST /api/reports/email
     */
    @PostMapping("/email")
    public ResponseEntity<ExportResponse> emailReport(@RequestBody EmailExportRequest request) {
        logger.info("Generic email report request - userId: {}, email: {}, type: {}, format: {}, startDate: {}, endDate: {}",
                request.getUserId(), request.getEmail(), request.getExportType(), request.getFormat(),
                request.getStartDate(), request.getEndDate());

        ExportResponse response = reportService.generateAndEmailReport(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Helper method to generate download response
     */
    private ResponseEntity<byte[]> generateDownloadResponse(ExportRequest request) {
        try {
            byte[] reportData = reportService.generateReport(request);
            String fileName = reportService.generateFileName(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", fileName);

            MediaType contentType = request.getFormat() == ExportRequest.ExportFormat.EXCEL ?
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") :
                    MediaType.APPLICATION_PDF;
            headers.setContentType(contentType);
            headers.setContentLength(reportData.length);

            logger.info("Report generated successfully - fileName: {}, size: {} bytes", fileName, reportData.length);
            return new ResponseEntity<>(reportData, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Error generating report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
