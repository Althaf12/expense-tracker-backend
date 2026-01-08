package com.expensetracker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Provides a fallback No-Op JavaMailSender when no {@link JavaMailSender} bean is configured.
 * This prevents application startup failure in environments where mail is intentionally disabled.
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    @Primary
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender noopJavaMailSender() {
        log.warn("No JavaMailSender configured - registering NoOp JavaMailSender. Mail will not be sent.");
        return new JavaMailSender() {
            private final Session session = Session.getDefaultInstance(new Properties());

            @Override
            public MimeMessage createMimeMessage() {
                return new MimeMessage(session);
            }

            @Override
            public MimeMessage createMimeMessage(InputStream contentStream) {
                try {
                    return new MimeMessage(session, contentStream);
                } catch (Exception e) {
                    log.warn("[NoOpMailSender] failed to create MimeMessage from stream: {}", e.getMessage());
                    return new MimeMessage(session);
                }
            }

            @Override
            public void send(MimeMessage mimeMessage) {
                try {
                    log.warn("[NoOpMailSender] would send MimeMessage to: {}",
                            Arrays.toString(mimeMessage.getAllRecipients()));
                } catch (Exception e) {
                    log.warn("[NoOpMailSender] would send MimeMessage (failed to read recipients): {}", e.getMessage());
                }
            }

            @Override
            public void send(MimeMessage... mimeMessages) {
                log.warn("[NoOpMailSender] would send {} mime messages", mimeMessages.length);
            }

            @Override
            public void send(SimpleMailMessage simpleMessage) {
                log.warn("[NoOpMailSender] would send SimpleMailMessage to: {}", Arrays.toString(simpleMessage.getTo()));
            }

            @Override
            public void send(SimpleMailMessage... simpleMessages) {
                log.warn("[NoOpMailSender] would send {} simple mail messages", simpleMessages.length);
            }
        };
    }
}
