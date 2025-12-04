package com.studentdetails.details.Service.ServiceImpl;
import com.studentdetails.details.Domain.EmailNotification;
import com.studentdetails.details.Repository.EmailNotificationRepository;
import com.studentdetails.details.Service.EmailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final int MAX_RETRIES = 3;
    private final JavaMailSender mailSender;
    private final EmailNotificationRepository emailNotificationRepository;
    private final TemplateEngine templateEngine;

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        // Only queue the email; actual sending is handled by scheduled job
        EmailNotification emailNotification = new EmailNotification();
        emailNotification.setToEmail(toEmail);
        emailNotification.setSubject(subject);
        emailNotification.setBody(body);
        emailNotification.setIsHtml(false);
        emailNotification.setStatus(EmailNotification.EmailStatus.PENDING);
        emailNotification.setSentTime(null);
        emailNotification.setRetryCount(0);
        emailNotification.setLastAttemptTime(null);
        emailNotification.setLastError(null);
        emailNotification = emailNotificationRepository.save(emailNotification);

        log.info("Queued email to {} with subject '{}'", toEmail, subject);

        // Attempt to send immediately so users don't need to wait for scheduler.
        // Scheduler will still retry failures later.
        sendEmailAsyncWithRetry(emailNotification);
    }

    @Override
    public void sendEmailWithTemplate(String toEmail, String subject, String templateName, Map<String, Object> templateVariables) {
        try {
            // Process the template with the given variables
            Context context = new Context();
            if (templateVariables != null) {
                templateVariables.forEach(context::setVariable);
            }
            
            // Add subject to context for template use
            context.setVariable("subject", subject);
            
            // Render the template
            String htmlContent = templateEngine.process("email/" + templateName, context);
            
            // Queue the email with HTML content
            EmailNotification emailNotification = new EmailNotification();
            emailNotification.setToEmail(toEmail);
            emailNotification.setSubject(subject);
            emailNotification.setBody(htmlContent);
            emailNotification.setIsHtml(true);
            emailNotification.setStatus(EmailNotification.EmailStatus.PENDING);
            emailNotification.setSentTime(null);
            emailNotification.setRetryCount(0);
            emailNotification.setLastAttemptTime(null);
            emailNotification.setLastError(null);
            emailNotification = emailNotificationRepository.save(emailNotification);

            log.info("Queued HTML email with template '{}' to {} with subject '{}'", templateName, toEmail, subject);

            // Attempt to send immediately so users don't need to wait for scheduler.
            // Scheduler will still retry failures later.
            sendEmailAsyncWithRetry(emailNotification);
        } catch (Exception e) {
            log.error("Error processing email template '{}' for {}: {}", templateName, toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to process email template: " + e.getMessage(), e);
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
        notifications.forEach(this::sendEmailAsyncWithRetry);
    }

    @Async
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
        } catch (Exception e) {
            notification.setStatus(EmailNotification.EmailStatus.FAILED);
            notification.setLastAttemptTime(LocalDateTime.now());
            notification.setLastError(e.getMessage());
            notification.setRetryCount(attempt);
            emailNotificationRepository.save(notification);

            log.error("Failed to send email id {} to {} on attempt {}",
                    notification.getId(), notification.getToEmail(), attempt, e);
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

