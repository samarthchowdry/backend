package com.studentdetails.details.Resources;

import com.studentdetails.details.Domain.DailyReportLog;
import com.studentdetails.details.Domain.EmailNotification;
import com.studentdetails.details.Domain.ReportScheduleConfig;
import com.studentdetails.details.Service.EmailService;
import com.studentdetails.details.Service.DailyStudentReportScheduler;
import com.studentdetails.details.Service.ProgressAnalyticsReportScheduler;
import com.studentdetails.details.Service.IndividualStudentReportScheduler;
import com.studentdetails.details.Repository.DailyReportLogRepository;
import com.studentdetails.details.Repository.ReportScheduleConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/admin/monitoring")
@RequiredArgsConstructor
public class AdminMonitoringController {

    private final EmailService emailService;
    private final DailyReportLogRepository dailyReportLogRepository;
    private final ReportScheduleConfigRepository reportScheduleConfigRepository;
    private final DailyStudentReportScheduler dailyReportScheduler;
    private final ProgressAnalyticsReportScheduler progressAnalyticsReportScheduler;
    private final IndividualStudentReportScheduler individualStudentReportScheduler;
    private final JavaMailSender mailSender;

    @GetMapping("/email-queue")
    public ResponseEntity<List<EmailNotification>> getEmailQueue(
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        // In a real application, you would enforce ADMIN role here similar to ReportController.
        return ResponseEntity.ok(emailService.getAllEmailNotifications());
    }

    @GetMapping("/daily-reports")
    public ResponseEntity<List<DailyReportLog>> getDailyReports(
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        return ResponseEntity.ok(dailyReportLogRepository.findAllByOrderByReportDateDesc());
    }

    @PostMapping("/email-queue/process")
    public ResponseEntity<Void> triggerEmailQueueProcessing(
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        emailService.processPendingEmails();
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/report-schedule")
    public ResponseEntity<ReportScheduleConfig> getReportSchedule(
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        ReportScheduleConfig config = reportScheduleConfigRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    ReportScheduleConfig newConfig = new ReportScheduleConfig();
                    newConfig.setReportHour(10);
                    newConfig.setReportMinute(45);
                    reportScheduleConfigRepository.save(newConfig);
                    return newConfig;
                });
        return ResponseEntity.ok(config);
    }

    @PutMapping("/report-schedule")
    public ResponseEntity<ReportScheduleConfig> updateReportSchedule(
            @RequestParam("hour") int hour,
            @RequestParam(value = "minute", defaultValue = "0") int minute,
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return ResponseEntity.badRequest().build();
        }
        ReportScheduleConfig config = reportScheduleConfigRepository.findAll().stream()
                .findFirst()
                .orElseGet(ReportScheduleConfig::new);
        config.setReportHour(hour);
        config.setReportMinute(minute);
        reportScheduleConfigRepository.save(config);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/daily-report/trigger")
    public ResponseEntity<String> triggerDailyReportManually(
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        try {
            dailyReportScheduler.generateAndSendDailyStudentReportManually();
            return ResponseEntity.ok("Daily report generation triggered. Check logs and email inbox.");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Failed to trigger daily report: " + e.getMessage());
        }
    }

    @PostMapping("/progress-analytics-report/trigger")
    public ResponseEntity<String> triggerProgressAnalyticsReportManually(
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        try {
            progressAnalyticsReportScheduler.generateAndSendProgressAnalyticsReportManually();
            return ResponseEntity.ok("Progress analytics report generation triggered. Check logs and email inbox.");
        } catch (Exception e) {
            String errorMsg = "Failed to trigger progress analytics report: " + e.getMessage();
            if (e.getCause() != null) {
                errorMsg += " (Cause: " + e.getCause().getMessage() + ")";
            }
            return ResponseEntity.status(500).body(errorMsg);
        }
    }

    @PostMapping("/individual-student-reports/trigger")
    public ResponseEntity<String> triggerIndividualStudentReportsManually(
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        try {
            individualStudentReportScheduler.sendIndividualStudentReportsManually();
            return ResponseEntity.ok("Individual student reports generation triggered. Reports will be sent to all students. Check logs for details.");
        } catch (Exception e) {
            String errorMsg = "Failed to trigger individual student reports: " + e.getMessage();
            if (e.getCause() != null) {
                errorMsg += " (Cause: " + e.getCause().getMessage() + ")";
            }
            return ResponseEntity.status(500).body(errorMsg);
        }
    }

    @PostMapping("/test-email")
    public ResponseEntity<String> testEmail(
            @RequestParam(value = "to", required = false) String toEmail,
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        try {
            // Use provided email or get admin email from database
            String recipient = toEmail;
            if (recipient == null || recipient.isBlank()) {
                // Get admin email from database
                recipient = progressAnalyticsReportScheduler.getAdminEmailForTesting();
                if (recipient == null || recipient.isBlank()) {
                    return ResponseEntity.status(400)
                            .body("No admin email found in database. Please provide 'to' parameter or ensure an admin has logged in.");
                }
            }
            
            // Send a simple test email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject("Test Email from Student Management System");
            message.setText("This is a test email to verify email configuration is working correctly.\n\n" +
                    "If you receive this email, the email system is configured properly.");
            
            mailSender.send(message);
            
            return ResponseEntity.ok("Test email sent successfully to: " + recipient + 
                    "\nPlease check your inbox and spam folder.");
        } catch (Exception e) {
            String errorMsg = "Failed to send test email: " + e.getMessage();
            if (e.getCause() != null) {
                errorMsg += " (Cause: " + e.getCause().getMessage() + ")";
            }
            return ResponseEntity.status(500).body(errorMsg);
        }
    }
}


