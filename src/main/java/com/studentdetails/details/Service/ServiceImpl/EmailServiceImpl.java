package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.Domain.EmailNotification;
import com.studentdetails.details.Repository.EmailNotificationRepository;
import com.studentdetails.details.Service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for email operations.
 * This class is used by Spring Framework for dependency injection.
 */
@RequiredArgsConstructor
@Service
@Slf4j
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class EmailServiceImpl implements EmailService {

    private static final int MAX_RETRIES = 3;
    private final JavaMailSender mailSender;
    private final EmailNotificationRepository emailNotificationRepository;
    private final TemplateEngine templateEngine;

    // Self-injection for async method calls (required for @Async to work via proxy)
    // Field injection is required here as constructor injection would cause circular dependency
    @Autowired
    @Lazy
    @SuppressWarnings("java:S6813") // Suppress field injection warning - self-injection requires field injection
    private EmailServiceImpl self;

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        // Only queue the email; actual sending is handled by scheduled job
        EmailNotification emailNotification = createEmailNotification(toEmail, subject, body, false);
        emailNotification = emailNotificationRepository.save(emailNotification);

        log.info("Queued email to {} with subject '{}'", toEmail, subject);

        // Attempt to send immediately so users don't need to wait for scheduler.
        // Scheduler will still retry failures later.
        self.sendEmailAsyncWithRetry(emailNotification);
    }

    @Override
    public void sendEmailWithTemplate(String toEmail, String subject, String templateName, Map<String, Object> templateVariables) {
        try {
            // Process the template with the given variables
            String htmlContent = processTemplate(templateName, subject, templateVariables);

            // Queue the email with HTML content
            EmailNotification emailNotification = createEmailNotification(toEmail, subject, htmlContent, true);
            emailNotification = emailNotificationRepository.save(emailNotification);

            log.info("Queued HTML email with template '{}' to {} with subject '{}'", templateName, toEmail, subject);

            // Attempt to send immediately so users don't need to wait for scheduler.
            // Scheduler will still retry failures later.
            self.sendEmailAsyncWithRetry(emailNotification);
        } catch (RuntimeException e) {
            log.error("Error processing email template '{}' for {}: {}", templateName, toEmail, e.getMessage(), e);
            throw new IllegalStateException("Failed to process email template: " + e.getMessage(), e);
        }
    }

    @Override
    public void processPendingEmails() {
        List<EmailNotification.EmailStatus> statusesToProcess = Arrays.asList(
                EmailNotification.EmailStatus.PENDING,
                EmailNotification.EmailStatus.FAILED
        );
        List<EmailNotification> notifications =
                emailNotificationRepository.findTop100ByStatusInOrderByIdAsc(statusesToProcess);

        if (notifications.isEmpty()) {
            log.info("No pending/failed emails to process");
            return;
        }

        log.info("Processing {} pending/failed email notifications", notifications.size());
        // Process emails in parallel using parallel stream for better performance
        notifications.parallelStream()
                .forEach(self::sendEmailAsyncWithRetry);
    }

    /**
     * Creates an email notification with the given parameters.
     *
     * @param toEmail the recipient email
     * @param subject the email subject
     * @param body the email body
     * @param isHtml whether the email is HTML
     * @return the created email notification
     */
    private EmailNotification createEmailNotification(String toEmail, String subject, String body, boolean isHtml) {
        EmailNotification emailNotification = new EmailNotification();
        emailNotification.setToEmail(toEmail);
        emailNotification.setSubject(subject);
        emailNotification.setBody(body);
        emailNotification.setIsHtml(isHtml);
        emailNotification.setStatus(EmailNotification.EmailStatus.PENDING);
        emailNotification.setSentTime(null);
        emailNotification.setRetryCount(0);
        emailNotification.setLastAttemptTime(null);
        emailNotification.setLastError(null);
        return emailNotification;
    }

    /**
     * Processes a Thymeleaf template with the given variables.
     *
     * @param templateName the template name
     * @param subject the email subject
     * @param templateVariables the template variables
     * @return the processed HTML content
     */
    private String processTemplate(String templateName, String subject, Map<String, Object> templateVariables) {
        Context context = new Context();
        if (templateVariables != null) {
            templateVariables.forEach(context::setVariable);
        }
        context.setVariable("subject", subject);
        return templateEngine.process("email/" + templateName, context);
    }

    /**
     * Handles email send failure by updating notification status and logging error.
     *
     * @param notification the email notification
     * @param attempt the attempt number
     * @param e the exception that occurred
     */
    private void handleEmailSendFailure(EmailNotification notification, int attempt, Exception e) {
        notification.setStatus(EmailNotification.EmailStatus.FAILED);
        notification.setLastAttemptTime(LocalDateTime.now());
        notification.setLastError(e.getMessage());
        notification.setRetryCount(attempt);
        emailNotificationRepository.save(notification);

        log.error("Failed to send email id {} to {} on attempt {}",
                notification.getId(), notification.getToEmail(), attempt, e);
    }

    @Async("emailTaskExecutor")
    protected void sendEmailAsyncWithRetry(EmailNotification notification) {
        if (notification.getRetryCount() >= MAX_RETRIES &&
                notification.getStatus() == EmailNotification.EmailStatus.FAILED) {
            log.warn("Skipping email id {} to {} - max retries reached",
                    notification.getId(), notification.getToEmail());
            return;
        }

        int attempt = notification.getRetryCount() + 1;
        log.info("Sending email id {} to {} attempt {}",
                notification.getId(), notification.getToEmail(), attempt);

        try {
            if (Boolean.TRUE.equals(notification.getIsHtml())) {
                // Send HTML email using MimeMessage
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setTo(notification.getToEmail());
                helper.setSubject(notification.getSubject());
                helper.setText(notification.getBody(), true); // true indicates HTML content
                mailSender.send(mimeMessage);
            } else {
                // Send plain text email
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(notification.getToEmail());
                message.setSubject(notification.getSubject());
                message.setText(notification.getBody());
                mailSender.send(message);
            }

            notification.setStatus(EmailNotification.EmailStatus.SENT);
            notification.setSentTime(LocalDateTime.now());
            notification.setLastAttemptTime(LocalDateTime.now());
            notification.setLastError(null);
            notification.setRetryCount(attempt);
            emailNotificationRepository.save(notification);

            log.info("Successfully sent email id {} to {}", notification.getId(), notification.getToEmail());
        } catch (MailException | jakarta.mail.MessagingException ex) {
            handleEmailSendFailure(notification, attempt, ex);
        }
    }

    @Override
    public List<EmailNotification> getAllEmailNotifications() {
        return emailNotificationRepository.findAllByOrderBySentTimeDesc();
    }

    @Override
    public void clearAllEmailNotifications() {
        emailNotificationRepository.deleteAll();
    }
}

