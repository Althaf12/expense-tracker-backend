package com.expensetracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final Logger logger = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender mailSender;
    private final String mailFrom;

    public MailService(JavaMailSender mailSender, @Value("${spring.mail.username:}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        // Log injection status at startup to help debug mail send problems
        if (this.mailSender == null) {
            logger.warn("MailService initialized WITHOUT JavaMailSender (mailSender is null)");
        } else {
            logger.info("MailService initialized with JavaMailSender implementation: {}", mailSender.getClass().getName());
        }
        logger.info("MailService configured mailFrom='{}'", this.mailFrom);
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (toEmail == null || toEmail.isBlank()) {
            logger.warn("sendPasswordResetEmail called with empty toEmail");
            return;
        }
        if (resetLink == null || resetLink.isBlank()) {
            logger.warn("sendPasswordResetEmail called with empty resetLink for {}", toEmail);
            return;
        }

        logger.info("Preparing password reset email to {}", toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Request");
        message.setText("Click the link below to reset your password:\n" + resetLink);
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }

        // Try sending with a small retry to handle transient network issues
        int maxAttempts = 2;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (mailSender == null) {
                    logger.error("JavaMailSender bean is null - cannot send email to {}", toEmail);
                    return;
                }
                logger.debug("Attempting to send email to {} (attempt {}/{})", toEmail, attempt, maxAttempts);
                mailSender.send(message);
                logger.info("Mail sent to : {}", toEmail);
                return;
            } catch (Exception ex) {
                logger.error("Failed to send mail to {} on attempt {}/{}: {}", toEmail, attempt, maxAttempts, ex.getMessage(), ex);
                if (attempt >= maxAttempts) {
                    logger.error("Giving up sending mail to {} after {} attempts", toEmail, attempt);
                    return;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted during mail retry backoff");
                    return;
                }
            }
        }
    }
}
