package com.studentdetails.details.Service;

import com.studentdetails.details.Domain.DailyReportLog;
import com.studentdetails.details.Domain.LoginInfo;
import com.studentdetails.details.Domain.UserRole;
import com.studentdetails.details.DTO.StudentProgressReportDTO;
import com.studentdetails.details.DTO.StudentProgressReportResponseDTO;
import com.studentdetails.details.Repository.DailyReportLogRepository;
import com.studentdetails.details.Repository.LoginInfoRepository;
import com.studentdetails.details.Service.NotificationService;
import com.studentdetails.details.Utility.CsvUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled job that generates a daily progress analytics report (from student-performance page)
 * and emails it to the admin at 10:50 AM.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProgressAnalyticsReportScheduler {

    private final ReportService reportService;
    private final JavaMailSender mailSender;
    private final DailyReportLogRepository dailyReportLogRepository;
    private final LoginInfoRepository loginInfoRepository;
    private final NotificationService notificationService;

    @Value("${spring.mail.username:samarthchowdry3@gmail.com}")
    private String mailUsername;

    /**
     * Runs every minute and checks whether it is time to generate the progress analytics report
     * at 11:00 AM. Ensures only one report per day.
     */
    @Scheduled(cron = "0 * * * * *")
    public void generateAndSendProgressAnalyticsReport() {
        LocalDate today = LocalDate.now();
        
        // Check if progress analytics report already sent today (only for scheduled runs)
        if (dailyReportLogRepository.findAll().stream()
                .anyMatch(log -> log.getReportDate().equals(today) && 
                               log.getFileName() != null && 
                               log.getFileName().contains("progress-analytics") &&
                               log.getStatus() == DailyReportLog.ReportStatus.SENT)) {
            log.debug("Progress analytics report for {} already sent today. Skipping.", today);
            return;
        }

        LocalTime now = LocalTime.now();
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();
        
        // Check if it's 11:00 AM
        if (currentHour != 11 || currentMinute != 0) {
            log.debug("Current time {}:{} does not match schedule 11:00. Skipping.", 
                    currentHour, currentMinute);
            return;
        }

        log.info("=== SCHEDULED: Starting progress analytics report generation at 11:00 AM ===");
        generateAndSendReport(today, false);
    }

    /**
     * Manually trigger progress analytics report generation.
     * Used by the admin monitoring endpoint.
     * Bypasses the duplicate check to allow re-sending.
     */
    public void generateAndSendProgressAnalyticsReportManually() {
        LocalDate today = LocalDate.now();
        log.info("=== MANUAL TRIGGER: Generating progress analytics report for {} ===", today);
        generateAndSendReport(today, true);
    }

    private void generateAndSendReport(LocalDate reportDate, boolean isManualTrigger) {
        String fileName = "progress-analytics-report-" +
                reportDate.format(DateTimeFormatter.ISO_DATE) + 
                (isManualTrigger ? "-manual-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) : "") +
                ".csv";

        DailyReportLog logEntry = new DailyReportLog();
        logEntry.setReportDate(reportDate);
        logEntry.setFileName(fileName);
        logEntry.setStatus(DailyReportLog.ReportStatus.GENERATED);
        logEntry.setGeneratedAt(LocalDateTime.now());
        dailyReportLogRepository.save(logEntry);
        log.info("Report log entry created: ID={}, FileName={}, Status=GENERATED", logEntry.getId(), fileName);

        try {
            log.info("Generating progress analytics report...");
            StudentProgressReportResponseDTO report = reportService.generateStudentProgressReport();
            log.info("Progress analytics report generated with {} students", report.getTotalStudents());

            byte[] csvBytes = buildCsv(report);
            log.info("CSV file created, size: {} bytes", csvBytes.length);

            // Get admin email from database (most recently logged in admin)
            String recipientEmail = CsvUtil.getAdminEmail(loginInfoRepository);
            if (recipientEmail == null || recipientEmail.isBlank()) {
                String errorMsg = "No admin user found in database. Please ensure an admin has logged in.";
                log.error("✗ {}", errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            log.info("✓ Admin email retrieved: {}", recipientEmail);
            log.info("Email configuration - From: {}, To: {}", mailUsername, recipientEmail);

            log.info("=== Attempting to send progress analytics report email ===");
            sendReportEmail(fileName, csvBytes, recipientEmail);

            logEntry.setStatus(DailyReportLog.ReportStatus.SENT);
            logEntry.setSentAt(LocalDateTime.now());
            logEntry.setErrorMessage(null);
            dailyReportLogRepository.save(logEntry);
            log.info("=== ✓ SUCCESS: Progress analytics report for {} successfully generated and emailed to {} ===", reportDate, recipientEmail);
            log.info("Report log entry updated: ID={}, Status=SENT, SentAt={}", logEntry.getId(), logEntry.getSentAt());

            // Create an in-app notification for the admin UI
            try {
                notificationService.createNotification(
                        "Progress analytics report emailed",
                        "Progress analytics report for " + reportDate + " has been emailed to " + recipientEmail
                );
            } catch (Exception notifError) {
                log.warn("Failed to create notification, but report was sent successfully", notifError);
            }
        } catch (Exception e) {
            log.error("=== ✗ FAILED: Error generating or sending progress analytics report for {} ===", reportDate);
            log.error("Error type: {}", e.getClass().getSimpleName());
            log.error("Error message: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Cause: {}", e.getCause().getMessage());
            }
            log.error("Full stack trace:", e);
            
            logEntry.setStatus(DailyReportLog.ReportStatus.FAILED);
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName() + ": " + (e.getCause() != null ? e.getCause().getMessage() : "Unknown error");
            }
            logEntry.setErrorMessage(errorMsg);
            dailyReportLogRepository.save(logEntry);
            log.error("Report log entry updated: ID={}, Status=FAILED, ErrorMessage={}", logEntry.getId(), errorMsg);
            
            throw new RuntimeException("Failed to send progress analytics report: " + errorMsg, e);
        }
    }

    private byte[] buildCsv(StudentProgressReportResponseDTO report) {
        StringBuilder csv = new StringBuilder();
        
        // Header row
        csv.append("Student Name,Branch,Total Assessments,Overall Average Score,Overall Percentage,Last Assessment Date\n");
        
        // Data rows
        if (report.getStudents() != null) {
            for (StudentProgressReportDTO student : report.getStudents()) {
                csv.append(CsvUtil.escapeCsvValue(CsvUtil.safeString(student.getStudentName()))).append(",");
                csv.append(CsvUtil.escapeCsvValue(CsvUtil.safeString(student.getBranch()))).append(",");
                csv.append(student.getTotalAssessments()).append(",");
                csv.append(CsvUtil.formatDouble(student.getOverallAverageScore())).append(",");
                csv.append(CsvUtil.formatDouble(student.getOverallPercentage())).append(",");
                csv.append(CsvUtil.escapeCsvValue(student.getLastAssessmentDate() != null ? student.getLastAssessmentDate().toString() : "")).append("\n");
            }
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    

    /**
     * Public method to get admin email for testing purposes.
     */
    public String getAdminEmailForTesting() {
        return CsvUtil.getAdminEmail(loginInfoRepository);
    }

    /**
     * Sends a test email to verify email configuration.
     */
    public void sendTestEmail(org.springframework.mail.SimpleMailMessage message) throws Exception {
        log.info("=== Sending test email ===");
        log.info("To: {}", message.getTo());
        log.info("Subject: {}", message.getSubject());
        try {
            mailSender.send(message);
            log.info("=== ✓ Test email sent successfully ===");
        } catch (Exception e) {
            log.error("=== ✗ Test email failed ===");
            log.error("Error: {}", e.getMessage(), e);
            throw e;
        }
    }


    private void sendReportEmail(String fileName, byte[] bytes, String recipientEmail) throws Exception {
        log.info("=== Preparing to send progress analytics report email ===");
        log.info("Recipient email: {}", recipientEmail);
            log.info("Email attachment size: {} bytes", bytes.length);
        log.info("From email: {}", mailUsername);
        
        if (bytes.length == 0) {
            throw new IllegalStateException("CSV file is empty. Cannot send email with empty attachment.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set from address
            String fromEmail = mailUsername;
            if (fromEmail == null || fromEmail.isBlank()) {
                fromEmail = "samarthchowdry3@gmail.com"; // fallback
            }
            helper.setFrom(fromEmail);
            log.info("From address set to: {}", fromEmail);

            // Ensure recipient is samarthchowdry3@gmail.com
            helper.setTo(recipientEmail);
            log.info("To address set to: {}", recipientEmail);
            
            String subject = "Progress Analytics Report - " + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            helper.setSubject(subject);
            log.info("Email subject: {}", subject);
            
            String emailBody = "Please find attached the progress analytics report (CSV format) from the student-performance dashboard.\n\n" +
                    "This is an automated report generated by the Student Management System at 11:00 AM daily.";
            helper.setText(emailBody, false);
            
            helper.addAttachment(fileName, () -> new java.io.ByteArrayInputStream(bytes), "text/csv");
            log.info("CSV attachment added: {} ({} bytes)", fileName, bytes.length);

            log.info("=== Attempting to send email ===");
            log.info("From: {}", fromEmail);
            log.info("To: {}", recipientEmail);
            log.info("Subject: {}", subject);
            log.info("Attachment: {} ({} bytes)", fileName, bytes.length);
            
            // Validate email addresses
            if (recipientEmail == null || recipientEmail.isBlank() || !recipientEmail.contains("@")) {
                throw new IllegalArgumentException("Invalid recipient email address: " + recipientEmail);
            }
            if (fromEmail == null || fromEmail.isBlank() || !fromEmail.contains("@")) {
                throw new IllegalArgumentException("Invalid sender email address: " + fromEmail);
            }
            
            log.info("Email addresses validated. Sending email...");
            
            // Send email with explicit error handling
            try {
                mailSender.send(message);
                log.info("=== ✓ EMAIL SENT SUCCESSFULLY ===");
                log.info("Email successfully sent to: {}", recipientEmail);
                log.info("Please check inbox and spam folder at: {}", recipientEmail);
                log.info("If email is not received, check:");
                log.info("  1. Spam/Junk folder");
                log.info("  2. Gmail app password is correct");
                log.info("  3. Email filters or rules");
            } catch (Exception sendException) {
                log.error("=== ✗ EMAIL SEND FAILED ===");
                log.error("Exception during mailSender.send(): {}", sendException.getClass().getSimpleName());
                log.error("Error message: {}", sendException.getMessage());
                if (sendException.getCause() != null) {
                    log.error("Root cause: {}", sendException.getCause().getMessage());
                    log.error("Root cause type: {}", sendException.getCause().getClass().getSimpleName());
                }
                log.error("Full exception:", sendException);
                throw sendException;
            }
        } catch (jakarta.mail.MessagingException me) {
            log.error("=== ✗ EMAIL MESSAGING ERROR ===");
            log.error("Error type: {}", me.getClass().getSimpleName());
            log.error("Error message: {}", me.getMessage());
            if (me.getCause() != null) {
                log.error("Cause: {}", me.getCause().getMessage());
            }
            log.error("Full stack trace:", me);
            throw new Exception("Failed to send email to " + recipientEmail + ": " + me.getMessage(), me);
        } catch (org.springframework.mail.MailException me) {
            log.error("=== ✗ SPRING MAIL ERROR ===");
            log.error("Error type: {}", me.getClass().getSimpleName());
            log.error("Error message: {}", me.getMessage());
            if (me.getCause() != null) {
                log.error("Cause: {}", me.getCause().getMessage());
            }
            log.error("Full stack trace:", me);
            throw new Exception("Failed to send email to " + recipientEmail + ": " + me.getMessage(), me);
        } catch (Exception e) {
            log.error("=== ✗ UNEXPECTED ERROR ===");
            log.error("Error type: {}", e.getClass().getSimpleName());
            log.error("Error message: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Cause: {}", e.getCause().getMessage());
            }
            log.error("Full stack trace:", e);
            throw new Exception("Unexpected error sending email to " + recipientEmail + ": " + e.getMessage(), e);
        }
    }
}

