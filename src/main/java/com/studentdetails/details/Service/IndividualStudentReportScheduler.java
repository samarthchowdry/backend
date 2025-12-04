package com.studentdetails.details.Service;

import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Domain.StudentMark;
import com.studentdetails.details.Repository.StudentMarkRepository;
import com.studentdetails.details.Repository.StudentRepository;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scheduled job that generates individual student performance reports (CSV format)
 * and emails them to each student daily at 11:00 AM.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IndividualStudentReportScheduler {

    private final StudentRepository studentRepository;
    private final StudentMarkRepository studentMarkRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:samarthchowdry3@gmail.com}")
    private String mailUsername;

    /**
     * Runs every minute and checks whether it is time to send individual student reports
     * at 11:00 AM. Ensures only one batch per day.
     */
    @Scheduled(cron = "0 * * * * *")
    public void sendIndividualStudentReports() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();
        
        // Check if it's 11:00 AM
        if (currentHour != 11 || currentMinute != 0) {
            log.debug("Current time {}:{} does not match schedule 11:00. Skipping individual student reports.", 
                    currentHour, currentMinute);
            return;
        }

        log.info("=== SCHEDULED: Starting individual student report generation at 11:00 AM ===");
        sendReportsToAllStudents(today, false);
    }

    /**
     * Manually trigger individual student report generation.
     * Used by the admin monitoring endpoint.
     */
    public void sendIndividualStudentReportsManually() {
        LocalDate today = LocalDate.now();
        log.info("=== MANUAL TRIGGER: Generating individual student reports for {} ===", today);
        sendReportsToAllStudents(today, true);
    }

    private void sendReportsToAllStudents(LocalDate reportDate, boolean isManualTrigger) {
        log.info("Fetching all students from database...");
        List<Student> students = studentRepository.findAll();
        
        if (students.isEmpty()) {
            log.warn("No students found in database. Skipping individual report generation.");
            return;
        }

        log.info("Found {} students. Generating and sending individual reports...", students.size());
        
        int successCount = 0;
        int failureCount = 0;
        List<String> failedStudents = new ArrayList<>();

        for (Student student : students) {
            try {
                if (student.getEmail() == null || student.getEmail().isBlank()) {
                    log.warn("Student {} (ID: {}) has no email address. Skipping.", 
                            student.getName(), student.getId());
                    failureCount++;
                    failedStudents.add(student.getName() + " (no email)");
                    continue;
                }

                log.info("Processing student: {} (ID: {}, Email: {})", 
                        student.getName(), student.getId(), student.getEmail());

                // Generate CSV report for this student
                byte[] csvBytes = generateIndividualStudentCsv(student);
                log.info("CSV generated for student {}: {} bytes", student.getName(), csvBytes.length);

                // Email the report to the student
                String fileName = generateFileName(student, reportDate, isManualTrigger);
                sendStudentReportEmail(fileName, csvBytes, student);

                successCount++;
                log.info("✓ Report sent successfully to student: {} ({})", student.getName(), student.getEmail());

            } catch (Exception e) {
                failureCount++;
                String errorMsg = e.getMessage();
                if (errorMsg == null || errorMsg.isBlank()) {
                    errorMsg = e.getClass().getSimpleName();
                }
                log.error("✗ Failed to send report to student {} ({}): {}", 
                        student.getName(), student.getEmail(), errorMsg, e);
                failedStudents.add(student.getName() + " (" + errorMsg + ")");
            }
        }

        log.info("=== Individual Student Reports Summary ===");
        log.info("Total students: {}", students.size());
        log.info("Successfully sent: {}", successCount);
        log.info("Failed: {}", failureCount);
        if (!failedStudents.isEmpty()) {
            log.warn("Failed students: {}", String.join(", ", failedStudents));
        }

        // Create an in-app notification for the admin
        try {
            String notificationMessage = String.format(
                "Individual student reports sent: %d successful, %d failed out of %d students",
                successCount, failureCount, students.size()
            );
            // Note: You may need to inject NotificationService if you want notifications
            log.info("Notification: {}", notificationMessage);
        } catch (Exception notifError) {
            log.warn("Failed to create notification", notifError);
        }
    }

    private byte[] generateIndividualStudentCsv(Student student) {
        StringBuilder csv = new StringBuilder();

        // Get student marks
        List<StudentMark> marks = studentMarkRepository.findByStudentIdOrderByAssessedOnDesc(student.getId());
        
        // Calculate summary statistics
        int totalAssessments = marks.size();
        double totalScore = marks.stream()
                .mapToDouble(m -> m.getScore() != null ? m.getScore() : 0.0)
                .sum();
        double totalMaxScore = marks.stream()
                .mapToDouble(m -> m.getMaxScore() != null ? m.getMaxScore() : 0.0)
                .sum();
        double averageScore = totalAssessments > 0 ? totalScore / totalAssessments : 0.0;
        double overallPercentage = totalMaxScore > 0 ? (totalScore / totalMaxScore) * 100.0 : 0.0;
        
        LocalDate lastAssessedOn = marks.stream()
                .map(StudentMark::getAssessedOn)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        // Summary section
        csv.append("Student Name,Branch,Total Assessments,Average Score,Overall Percentage,Last Assessment Date\n");
        csv.append(escapeCsvValue(student.getName() != null ? student.getName() : "")).append(",");
        csv.append(escapeCsvValue(student.getBranch() != null ? student.getBranch() : "")).append(",");
        csv.append(totalAssessments).append(",");
        csv.append(String.format("%.2f", averageScore)).append(",");
        csv.append(String.format("%.2f", overallPercentage)).append(",");
        csv.append(escapeCsvValue(lastAssessedOn != null ? lastAssessedOn.toString() : "")).append("\n");
        csv.append("\n"); // Empty row

        // Courses section
        if (student.getCourses() != null && !student.getCourses().isEmpty()) {
            csv.append("Courses\n");
            int courseIndex = 1;
            for (var course : student.getCourses()) {
                csv.append(courseIndex).append(",");
                csv.append(escapeCsvValue(course.getName() != null ? course.getName() : "")).append("\n");
                courseIndex++;
            }
            csv.append("\n"); // Empty row
        }

        // Subjects section
        csv.append("Subjects\n");
        csv.append("Subject,Assessments,Total Score,Total Max Score,Average %\n");

        // Aggregate marks by subject
        Map<String, SubjectSummary> subjectMap = new HashMap<>();
        for (StudentMark mark : marks) {
            String subject = mark.getSubject() != null ? mark.getSubject() : "Unknown";
            SubjectSummary summary = subjectMap.computeIfAbsent(subject, k -> new SubjectSummary(subject));
            summary.assessments++;
            summary.totalScore += (mark.getScore() != null ? mark.getScore() : 0.0);
            summary.totalMaxScore += (mark.getMaxScore() != null ? mark.getMaxScore() : 0.0);
        }

        // Sort by percentage (descending)
        List<SubjectSummary> subjectSummaries = new ArrayList<>(subjectMap.values());
        subjectSummaries.sort((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()));

        // Write subject rows
        for (SubjectSummary summary : subjectSummaries) {
            csv.append(escapeCsvValue(summary.subject)).append(",");
            csv.append(summary.assessments).append(",");
            csv.append(String.format("%.2f", summary.totalScore)).append(",");
            csv.append(String.format("%.2f", summary.totalMaxScore)).append(",");
            csv.append(String.format("%.2f", summary.getPercentage())).append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static class SubjectSummary {
        String subject;
        int assessments = 0;
        double totalScore = 0.0;
        double totalMaxScore = 0.0;

        SubjectSummary(String subject) {
            this.subject = subject;
        }

        double getPercentage() {
            return totalMaxScore > 0 ? (totalScore / totalMaxScore) * 100.0 : 0.0;
        }
    }

    private String generateFileName(Student student, LocalDate reportDate, boolean isManualTrigger) {
        String safeName = (student.getName() != null ? student.getName() : "student")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        String timestamp = isManualTrigger 
                ? "-manual-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
                : "";
        return safeName + "-subject-breakdown-" + reportDate.format(DateTimeFormatter.ISO_DATE) + timestamp + ".csv";
    }

    private void sendStudentReportEmail(String fileName, byte[] csvBytes, Student student) throws Exception {
        log.info("=== Preparing to send individual student report email ===");
        log.info("Recipient: {} ({})", student.getName(), student.getEmail());
        log.info("Email attachment size: {} bytes", csvBytes.length);
        log.info("From email: {}", mailUsername);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(student.getEmail());
        helper.setFrom(mailUsername);
        helper.setSubject("Your Student Performance Report - " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        
        String emailBody = String.format(
            "Dear %s,\n\n" +
            "Please find attached your individual student performance report.\n\n" +
            "This report includes:\n" +
            "- Your overall performance summary\n" +
            "- Course enrollment details\n" +
            "- Subject-wise performance breakdown\n\n" +
            "If you have any questions, please contact your administrator.\n\n" +
            "Best regards,\n" +
            "Student Management System",
            student.getName() != null ? student.getName() : "Student"
        );
        
        helper.setText(emailBody);
        helper.addAttachment(fileName, () -> new java.io.ByteArrayInputStream(csvBytes), "text/csv");

        log.info("Sending email to: {}", student.getEmail());
        mailSender.send(message);
        log.info("✓ Email sent successfully to: {}", student.getEmail());
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

