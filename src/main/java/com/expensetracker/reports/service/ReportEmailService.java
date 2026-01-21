package com.expensetracker.reports.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportEmailService {

    private static final Logger logger = LoggerFactory.getLogger(ReportEmailService.class);
    private static final long MAX_ATTACHMENT_SIZE = 20 * 1024 * 1024; // 20MB in bytes
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired(required = false)
    public ReportEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Check if mail service is available
     */
    public boolean isMailServiceAvailable() {
        return mailSender != null;
    }

    /**
     * Send report via email. If file exceeds 20MB, split and send multiple emails.
     *
     * @param toEmail     Recipient email
     * @param reportData  Report file data
     * @param fileName    File name
     * @param reportType  Type of report (Expenses/Income/Both)
     * @param startDate   Start date of the report
     * @param endDate     End date of the report
     * @return Number of emails sent
     */
    public int sendReport(String toEmail, byte[] reportData, String fileName,
                           String reportType, LocalDate startDate, LocalDate endDate) throws MessagingException {

        if (mailSender == null) {
            throw new IllegalStateException("Mail service is not configured");
        }

        logger.info("Preparing to send report to: {}, file size: {} bytes", toEmail, reportData.length);

        if (reportData.length <= MAX_ATTACHMENT_SIZE) {
            // Send single email
            sendSingleEmail(toEmail, reportData, fileName, reportType, startDate, endDate, 1, 1);
            return 1;
        } else {
            // Split and send multiple emails
            return sendSplitEmails(toEmail, reportData, fileName, reportType, startDate, endDate);
        }
    }

    private void sendSingleEmail(String toEmail, byte[] reportData, String fileName,
                                  String reportType, LocalDate startDate, LocalDate endDate,
                                  int partNumber, int totalParts) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);

        String subject = String.format("Expense Tracker Report - %s (%s to %s)",
                reportType, startDate.format(DATE_FORMATTER), endDate.format(DATE_FORMATTER));

        if (totalParts > 1) {
            subject += String.format(" [Part %d of %d]", partNumber, totalParts);
        }
        helper.setSubject(subject);

        String body = buildEmailBody(reportType, startDate, endDate, partNumber, totalParts);
        helper.setText(body, true);

        String attachmentName = totalParts > 1 ?
                fileName.replace(".", "_part" + partNumber + ".") : fileName;
        helper.addAttachment(attachmentName, new ByteArrayResource(reportData));

        mailSender.send(message);
        logger.info("Email sent successfully to: {} (part {} of {})", toEmail, partNumber, totalParts);
    }

    private int sendSplitEmails(String toEmail, byte[] reportData, String fileName,
                                 String reportType, LocalDate startDate, LocalDate endDate) throws MessagingException {

        List<byte[]> chunks = splitData(reportData, MAX_ATTACHMENT_SIZE);
        int totalParts = chunks.size();

        logger.info("Report file size {} bytes exceeds limit, splitting into {} parts",
                reportData.length, totalParts);

        for (int i = 0; i < chunks.size(); i++) {
            sendSingleEmail(toEmail, chunks.get(i), fileName, reportType,
                    startDate, endDate, i + 1, totalParts);
        }

        return totalParts;
    }

    private List<byte[]> splitData(byte[] data, long maxSize) {
        List<byte[]> chunks = new ArrayList<>();
        int offset = 0;

        while (offset < data.length) {
            int chunkSize = (int) Math.min(maxSize, data.length - offset);
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(data, offset, chunk, 0, chunkSize);
            chunks.add(chunk);
            offset += chunkSize;
        }

        return chunks;
    }

    private String buildEmailBody(String reportType, LocalDate startDate, LocalDate endDate,
                                   int partNumber, int totalParts) {
        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<h2>Expense Tracker Report</h2>");
        body.append("<p>Please find attached your <strong>").append(reportType).append("</strong> report.</p>");
        body.append("<p><strong>Report Period:</strong> ")
                .append(startDate.format(DATE_FORMATTER))
                .append(" to ")
                .append(endDate.format(DATE_FORMATTER))
                .append("</p>");

        if (totalParts > 1) {
            body.append("<p><strong>Note:</strong> Due to file size limitations, this report has been split into ")
                    .append(totalParts)
                    .append(" parts. This is part ")
                    .append(partNumber)
                    .append(" of ")
                    .append(totalParts)
                    .append(".</p>");
        }

        body.append("<p>This is an automated email from Expense Tracker. Please do not reply to this email.</p>");
        body.append("<br><p>Best regards,<br>Expense Tracker Team</p>");
        body.append("</body></html>");

        return body.toString();
    }
}
