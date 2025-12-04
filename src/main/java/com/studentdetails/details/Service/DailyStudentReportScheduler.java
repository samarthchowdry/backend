package com.studentdetails.details.Service;

import com.studentdetails.details.Domain.DailyReportLog;
import com.studentdetails.details.Domain.ReportScheduleConfig;
import com.studentdetails.details.DTO.StudentProgressReportDTO;
import com.studentdetails.details.DTO.StudentProgressReportResponseDTO;
import com.studentdetails.details.Repository.DailyReportLogRepository;
import com.studentdetails.details.Repository.LoginInfoRepository;
import com.studentdetails.details.Repository.ReportScheduleConfigRepository;
import com.studentdetails.details.Service.NotificationService;
import com.studentdetails.details.Utility.CsvUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled job that generates a daily student progress Excel report and emails it to the admin.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyStudentReportScheduler {

    private final ReportService reportService;
    private final JavaMailSender mailSender;
    private final DailyReportLogRepository dailyReportLogRepository;
    private final ReportScheduleConfigRepository reportScheduleConfigRepository;
    private final LoginInfoRepository loginInfoRepository;
    private final NotificationService notificationService;

    @Value("${spring.mail.username:samarthchowdry3@gmail.com}")
    private String mailUsername;
    
    @Value("${app.report.admin-email:samarthchowdry3@gmail.com}")
    private String fallbackAdminEmail;

    /**
     * Checks on application startup if scheduled time has passed or it's past 11:00 PM and no report was sent today.
     * If so, sends the report immediately to ensure daily delivery.
     */
    @PostConstruct
    public void checkAndSendReportOnStartup() {
        try {
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            
            // Check if a report was already successfully sent today
            boolean reportAlreadySent = dailyReportLogRepository.findAll().stream()
                    .anyMatch(log -> log.getReportDate().equals(today) && 
                                   log.getStatus() == DailyReportLog.ReportStatus.SENT);
            
            if (reportAlreadySent) {
                log.info("STARTUP CHECK: Report already sent today. No action needed.");
                return;
            }
            
            int[] schedule = getConfiguredReportSchedule();
            int configuredHour = schedule[0];
            int configuredMinute = schedule[1];
            LocalTime scheduledTime = LocalTime.of(configuredHour, configuredMinute);
            
            // Check if scheduled time has passed or it's after 11:00 PM
            boolean scheduledTimePassed = now.isAfter(scheduledTime);
            boolean isAfter11PM = now.getHour() >= 23;
            
            if (scheduledTimePassed || isAfter11PM) {
                String reason = isAfter11PM ? "after 11:00 PM" : "scheduled time (" + scheduledTime + ") has passed";
                log.warn("⚠️ STARTUP CHECK: Server started {} (current time: {}). " +
                        "No report sent today. Sending report now...", reason, now);
                try {
                    generateAndSendReport(today);
                    log.info("✓ STARTUP CHECK: Daily report successfully sent on startup.");
                } catch (Exception e) {
                    log.error("✗ STARTUP CHECK: Failed to send report on startup. Error: {}", e.getMessage(), e);
                }
            } else {
                log.debug("STARTUP CHECK: Current time is {} (scheduled time {} not reached yet). Scheduled tasks will handle report sending.", 
                        now, scheduledTime);
            }
        } catch (Exception e) {
            log.error("Error during startup check for daily report: {}", e.getMessage(), e);
            // Don't fail startup if this check fails
        }
    }

    /**
     * Runs every minute and checks whether it is time to generate the daily report
     * based on the configured hour and minute. Ensures only one report per day.
     * Also includes fallback: if scheduled time has passed or it's after 11:00 PM and no report was sent, send it immediately.
     */
    @Scheduled(cron = "0 * * * * *")
    public void generateAndSendDailyStudentReport() {
        LocalDate today = LocalDate.now();
        
        // Check if a report was already successfully sent today
        boolean reportAlreadySent = dailyReportLogRepository.findAll().stream()
                .anyMatch(log -> log.getReportDate().equals(today) && 
                               log.getStatus() == DailyReportLog.ReportStatus.SENT);
        
        if (reportAlreadySent) {
            log.debug("Daily student report for {} already sent. Skipping.", today);
            return;
        }

        int[] schedule = getConfiguredReportSchedule();
        int configuredHour = schedule[0];
        int configuredMinute = schedule[1];
        
        LocalTime now = LocalTime.now();
        LocalTime scheduledTime = LocalTime.of(configuredHour, configuredMinute);
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();
        
        // Check if it's the exact scheduled time (e.g., 10:45 AM)
        boolean isScheduledTime = (currentHour == configuredHour && currentMinute == configuredMinute);
        
        // Check if scheduled time has passed (but before 11 PM) - send immediately
        boolean scheduledTimePassed = now.isAfter(scheduledTime) && currentHour < 23;
        
        // Fallback: If it's 11:00 PM or later and no report was sent today, send it now
        boolean isAfter11PM = currentHour >= 23;
        
        if (!isScheduledTime && !scheduledTimePassed && !isAfter11PM) {
            log.debug("Current time {}:{} - scheduled time {}:{} not reached yet. Skipping.", 
                    currentHour, currentMinute, configuredHour, configuredMinute);
            return;
        }
        
        if (isScheduledTime) {
            log.info("Scheduled time reached: {}:{}. Generating daily report.", configuredHour, configuredMinute);
        } else if (scheduledTimePassed) {
            log.warn("⚠️ SCHEDULED TIME PASSED: Current time is {}:{} (scheduled was {}:{}). " +
                    "No report sent today. Sending report now.", 
                    currentHour, currentMinute, configuredHour, configuredMinute);
        } else if (isAfter11PM) {
            log.warn("⚠️ FALLBACK TRIGGERED: Current time is {}:{} (after 11:00 PM) and no report sent today. Sending report now.", 
                    currentHour, currentMinute);
        }

        try {
            generateAndSendReport(today);
        } catch (Exception e) {
            log.error("Failed to generate/send daily report. Error: {}", e.getMessage(), e);
            // Don't rethrow - let it retry on next minute
        }
    }

    /**
     * Dedicated fallback scheduler that runs at exactly 11:00 PM every day.
     * This ensures the daily report is sent even if the server ran late or the scheduled time was missed.
     * Cron format: second minute hour day-of-month month day-of-week
     * "0 0 23 * * *" = Every day at 11:00:00 PM
     */
    @Scheduled(cron = "0 0 23 * * *")
    public void generateAndSendDailyStudentReportFallback() {
        LocalDate today = LocalDate.now();
        
        log.info("=== 11:00 PM FALLBACK SCHEDULER TRIGGERED ===");
        
        // Check if a report was already successfully sent today
        boolean reportAlreadySent = dailyReportLogRepository.findAll().stream()
                .anyMatch(log -> log.getReportDate().equals(today) && 
                               log.getStatus() == DailyReportLog.ReportStatus.SENT);
        
        if (reportAlreadySent) {
            log.info("Daily student report for {} already sent today. Fallback scheduler skipping.", today);
            return;
        }
        
        // Check if there's a failed or incomplete report
        Optional<DailyReportLog> existingReport = dailyReportLogRepository.findAll().stream()
                .filter(log -> log.getReportDate().equals(today))
                .findFirst();
        
        if (existingReport.isPresent()) {
            DailyReportLog.ReportStatus status = existingReport.get().getStatus();
            if (status == DailyReportLog.ReportStatus.FAILED) {
                log.warn("⚠️ Found FAILED report entry for today. Retrying at 11:00 PM fallback.");
            } else if (status == DailyReportLog.ReportStatus.GENERATED) {
                log.warn("⚠️ Found GENERATED (but not sent) report entry for today. Retrying at 11:00 PM fallback.");
            }
        } else {
            log.warn("⚠️ No report was sent today. Server may have run late. Sending report now at 11:00 PM fallback.");
        }
        
        try {
            generateAndSendReport(today);
            log.info("=== ✓ 11:00 PM FALLBACK: Daily report successfully sent ===");
        } catch (Exception e) {
            log.error("=== ✗ 11:00 PM FALLBACK: Failed to send daily report ===", e);
            // Don't rethrow - we've logged the error, and manual trigger can be used if needed
        }
    }

    /**
     * Manually trigger report generation (bypasses date check and time check).
     * Used by the admin monitoring endpoint.
     */
    public void generateAndSendDailyStudentReportManually() {
        LocalDate today = LocalDate.now();
        log.info("Manual trigger: Generating daily report for {}", today);
        generateAndSendReport(today);
    }

    private void generateAndSendReport(LocalDate reportDate) {

        String fileName = "student-progress-report-" +
                reportDate.format(DateTimeFormatter.ISO_DATE) + ".xlsx";

        // Check if report already exists for this date
        DailyReportLog existingLog = dailyReportLogRepository.findAll().stream()
                .filter(log -> log.getReportDate().equals(reportDate))
                .findFirst()
                .orElse(null);

        DailyReportLog logEntry = existingLog;
        if (logEntry == null) {
            logEntry = new DailyReportLog();
            logEntry.setReportDate(reportDate);
            logEntry.setFileName(fileName);
        } else {
            log.info("Updating existing report log entry for {}", reportDate);
        }

        logEntry.setStatus(DailyReportLog.ReportStatus.GENERATED);
        logEntry.setGeneratedAt(LocalDateTime.now());
        dailyReportLogRepository.save(logEntry);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            log.info("Generating student progress report...");
            StudentProgressReportResponseDTO report = reportService.generateStudentProgressReport();
            log.info("Report generated with {} students", report.getTotalStudents());

            buildWorkbook(workbook, report);
            workbook.write(out);
            log.info("Excel workbook created, size: {} bytes", out.size());

            // Get admin email from database (most recently logged in admin)
            String recipientEmail = CsvUtil.getAdminEmail(loginInfoRepository);
            if (recipientEmail == null || recipientEmail.isBlank()) {
                log.warn("No admin user found in database. Using fallback admin email: {}", fallbackAdminEmail);
                recipientEmail = fallbackAdminEmail;
                if (recipientEmail == null || recipientEmail.isBlank()) {
                    throw new IllegalStateException("No admin user found in database and no fallback email configured. Please ensure an admin has logged in or configure app.report.admin-email.");
                }
            }
            log.info("Daily student report will be sent to admin: {}", recipientEmail);

            log.info("Sending report email to: {}", recipientEmail);
            sendReportEmail(fileName, out.toByteArray(), recipientEmail);

            logEntry.setStatus(DailyReportLog.ReportStatus.SENT);
            logEntry.setSentAt(LocalDateTime.now());
            logEntry.setErrorMessage(null);
            dailyReportLogRepository.save(logEntry);
            log.info("✓ Daily student report for {} successfully generated and emailed to {}", reportDate, recipientEmail);

            // Create an in-app notification for the admin UI
            try {
                notificationService.createNotification(
                        "Daily report emailed",
                        "Student progress report for " + reportDate + " has been emailed to " + recipientEmail
                );
            } catch (Exception notifError) {
                log.warn("Failed to create notification, but report was sent successfully", notifError);
            }
        } catch (Exception e) {
            log.error("✗ Failed to generate or send daily student report for {}", reportDate, e);
            logEntry.setStatus(DailyReportLog.ReportStatus.FAILED);
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName() + ": " + (e.getCause() != null ? e.getCause().getMessage() : "Unknown error");
            }
            logEntry.setErrorMessage(errorMsg);
            dailyReportLogRepository.save(logEntry);
            throw new RuntimeException("Failed to send daily report: " + errorMsg, e);
        }
    }

    private void buildWorkbook(Workbook workbook, StudentProgressReportResponseDTO report) {
        Sheet sheet = workbook.createSheet("Student Progress");
        int rowIdx = 0;

        // Header
        Row header = sheet.createRow(rowIdx++);
        String[] columns = new String[]{
                "Student ID",
                "Student Name",
                "Branch",
                "Total Assessments",
                "Overall Average Score",
                "Overall Percentage",
                "Last Assessment Date"
        };
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Data rows
        if (report.getStudents() != null) {
            for (StudentProgressReportDTO student : report.getStudents()) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(
                        student.getStudentId() != null ? student.getStudentId() : 0L);
                row.createCell(col++).setCellValue(CsvUtil.safeString(student.getStudentName()));
                row.createCell(col++).setCellValue(CsvUtil.safeString(student.getBranch()));
                row.createCell(col++).setCellValue(student.getTotalAssessments());
                if (student.getOverallAverageScore() != null) {
                    row.createCell(col++).setCellValue(student.getOverallAverageScore());
                } else {
                    row.createCell(col++).setBlank();
                }
                if (student.getOverallPercentage() != null) {
                    row.createCell(col++).setCellValue(student.getOverallPercentage());
                } else {
                    row.createCell(col++).setBlank();
                }
                if (student.getLastAssessmentDate() != null) {
                    row.createCell(col).setCellValue(student.getLastAssessmentDate().toString());
                } else {
                    row.createCell(col).setBlank();
                }
            }
        }

        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private int[] getConfiguredReportSchedule() {
        // Returns [hour, minute]. Default is 10:45 AM.
        ReportScheduleConfig config = reportScheduleConfigRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    ReportScheduleConfig newConfig = new ReportScheduleConfig();
                    newConfig.setReportHour(10);
                    newConfig.setReportMinute(45);
                    reportScheduleConfigRepository.save(newConfig);
                    return newConfig;
                });
        return new int[]{config.getReportHour(), config.getReportMinute()};
    }


    private void sendReportEmail(String fileName, byte[] bytes, String recipientEmail) throws Exception {
        log.info("Preparing to send daily report email to: {}", recipientEmail);
        log.info("Email attachment size: {} bytes", bytes.length);
        
        if (bytes.length == 0) {
            throw new IllegalStateException("Excel file is empty. Cannot send email with empty attachment.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set from address (use the configured mail username)
            if (mailUsername == null || mailUsername.isBlank()) {
                mailUsername = "samarthchowdry3@gmail.com"; // fallback
            }
            helper.setFrom(mailUsername);

            helper.setTo(recipientEmail);
            helper.setSubject("Daily Student Progress Report - " + LocalDate.now().format(DateTimeFormatter.ISO_DATE));
            helper.setText("Please find attached the daily student progress report.\n\n" +
                    "This is an automated report generated by the Student Management System.", false);
            helper.addAttachment(fileName, () -> new java.io.ByteArrayInputStream(bytes));

            log.info("Attempting to send email with attachment '{}' ({} bytes) to {}", fileName, bytes.length, recipientEmail);
            mailSender.send(message);
            log.info("✓ Email sent successfully to {}", recipientEmail);
        } catch (jakarta.mail.MessagingException me) {
            log.error("Email messaging error: {}", me.getMessage(), me);
            throw new Exception("Failed to send email: " + me.getMessage(), me);
        } catch (org.springframework.mail.MailException me) {
            log.error("Spring mail error: {}", me.getMessage(), me);
            throw new Exception("Failed to send email: " + me.getMessage(), me);
        } catch (Exception e) {
            log.error("Unexpected error sending email: {}", e.getMessage(), e);
            throw new Exception("Unexpected error sending email: " + e.getMessage(), e);
        }
    }
}


