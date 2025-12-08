package com.studentdetails.details.Service;

import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Domain.StudentMark;
import com.studentdetails.details.Repository.StudentMarkRepository;
import com.studentdetails.details.Repository.StudentRepository;
import com.studentdetails.details.Utility.CsvUtil;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
    // Thread pool for parallel email sending (optimized for I/O-bound tasks)
    private final Executor emailExecutor = Executors.newFixedThreadPool(20);
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


//  Manually trigger individual student report generation.
//  Used by the admin monitoring endpoint.

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

        log.info("Found {} students. Generating and sending individual reports in parallel...", students.size());

        // Filter students with valid emails
        List<Student> studentsWithEmail = students.stream()
                .filter(s -> s.getEmail() != null && !s.getEmail().isBlank())
                .collect(Collectors.toList());

        if (studentsWithEmail.isEmpty()) {
            log.warn("No students with valid email addresses found.");
            return;
        }

        // OPTIMIZATION: Batch fetch all marks for all students in a single query (eliminates N+1)
        List<Long> studentIds = studentsWithEmail.stream()
                .map(Student::getId)
                .collect(Collectors.toList());

        log.info("Batch fetching marks for {} students...", studentIds.size());
        List<StudentMark> allMarks = studentMarkRepository.findByStudentIdInOrderByStudentIdAndAssessedOnDesc(studentIds);

        // Group marks by student ID for O(1) lookup
        Map<Long, List<StudentMark>> marksByStudentId = allMarks.stream()
                .collect(Collectors.groupingBy(m -> m.getStudent().getId()));

        log.info("Fetched {} marks for {} students. Starting parallel email processing...",
                allMarks.size(), marksByStudentId.size());

        // Process emails in parallel using CompletableFuture
        List<CompletableFuture<EmailResult>> futures = studentsWithEmail.stream()
                .map(student -> CompletableFuture.supplyAsync(() -> {
                    try {
                        log.debug("Processing student: {} (ID: {}, Email: {})",
                                student.getName(), student.getId(), student.getEmail());

                        // Get marks for this student from the pre-fetched map
                        List<StudentMark> studentMarks = marksByStudentId.getOrDefault(
                                student.getId(), Collections.emptyList());

                        // Generate CSV report
                        byte[] csvBytes = generateIndividualStudentCsv(student, studentMarks);
                        log.debug("CSV generated for student {}: {} bytes", student.getName(), csvBytes.length);

                        // Email the report
                        String fileName = generateFileName(student, reportDate, isManualTrigger);
                        sendStudentReportEmail(fileName, csvBytes, student);

                        log.debug("✓ Report sent successfully to student: {} ({})",
                                student.getName(), student.getEmail());
                        return new EmailResult(student.getName(), student.getEmail(), true, null);
                    } catch (Exception ex) {
                        String errorMsg = buildErrorMessage(ex);
                        log.error("✗ Failed to send report to student {} ({}): {}",
                                student.getName(), student.getEmail(), errorMsg, ex);
                        return new EmailResult(student.getName(), student.getEmail(), false, errorMsg);
                    }
                }, emailExecutor))
                .collect(Collectors.toList());

        // Wait for all emails to complete and collect results
        @SuppressWarnings("unchecked")
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        int successCount = 0;
        int failureCount = 0;
        List<String> failedStudents = new ArrayList<>();

        try {
            allFutures.join(); // Wait for all to complete

            for (CompletableFuture<EmailResult> future : futures) {
                try {
                    EmailResult result = future.get();
                    if (result.success) {
                        successCount++;
                    } else {
                        failureCount++;
                        failedStudents.add(result.studentName + " (" + result.error + ")");
                    }
                } catch (java.util.concurrent.ExecutionException | InterruptedException ex) {
                    log.error("Error getting email result from future", ex);
                    failureCount++;
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
            }
        } catch (Exception ex) {
            log.error("Error processing email futures", ex);
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
        } catch (Exception _) {
            log.warn("Failed to create notification");
        }
    }

    /**
     * Generate CSV report for a student using pre-fetched marks.
     * This eliminates the need for individual database queries.
     */
    private byte[] generateIndividualStudentCsv(Student student, List<StudentMark> marks) {
        StringBuilder csv = new StringBuilder();

        // Calculate summary statistics
        int totalAssessments = marks.size();
        double totalScore = marks.stream()
                .mapToDouble(m -> CsvUtil.safeValue(m.getScore(), 0.0))
                .sum();
        double totalMaxScore = marks.stream()
                .mapToDouble(m -> CsvUtil.safeValue(m.getMaxScore(), 0.0))
                .sum();
        double averageScore = CsvUtil.calculateAverage(totalScore, totalAssessments);
        double overallPercentage = CsvUtil.calculatePercentage(totalScore, totalMaxScore);

        LocalDate lastAssessedOn = marks.stream()
                .map(StudentMark::getAssessedOn)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        // Summary section
        csv.append("Student Name,Branch,Total Assessments,Average Score,Overall Percentage,Last Assessment Date\n");
        csv.append(CsvUtil.escapeCsvValue(CsvUtil.safeString(student.getName()))).append(",");
        csv.append(CsvUtil.escapeCsvValue(CsvUtil.safeString(student.getBranch()))).append(",");
        csv.append(totalAssessments).append(",");
        csv.append(CsvUtil.formatDouble(averageScore)).append(",");
        csv.append(CsvUtil.formatDouble(overallPercentage)).append(",");
        csv.append(CsvUtil.escapeCsvValue(lastAssessedOn != null ? lastAssessedOn.toString() : "")).append("\n");
        csv.append("\n"); // Empty row

        // Courses section
        if (student.getCourses() != null && !student.getCourses().isEmpty()) {
            csv.append("Courses\n");
            int courseIndex = 1;
            for (var course : student.getCourses()) {
                csv.append(courseIndex).append(",");
                csv.append(CsvUtil.escapeCsvValue(CsvUtil.safeString(course.getName()))).append("\n");
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
            String subject = CsvUtil.safeValue(mark.getSubject(), "Unknown");
            SubjectSummary summary = subjectMap.computeIfAbsent(subject, k -> new SubjectSummary(subject));
            summary.assessments++;
            summary.totalScore += CsvUtil.safeValue(mark.getScore(), 0.0);
            summary.totalMaxScore += CsvUtil.safeValue(mark.getMaxScore(), 0.0);
        }

        // Sort by percentage (descending)
        List<SubjectSummary> subjectSummaries = new ArrayList<>(subjectMap.values());
        subjectSummaries.sort((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()));

        // Write subject rows
        for (SubjectSummary summary : subjectSummaries) {
            csv.append(CsvUtil.escapeCsvValue(summary.subject)).append(",");
            csv.append(summary.assessments).append(",");
            csv.append(CsvUtil.formatDouble(summary.totalScore)).append(",");
            csv.append(CsvUtil.formatDouble(summary.totalMaxScore)).append(",");
            csv.append(CsvUtil.formatDouble(summary.getPercentage())).append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
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

    private static class SubjectSummary {
        String subject;
        int assessments = 0;
        double totalScore = 0.0;
        double totalMaxScore = 0.0;

        SubjectSummary(String subject) {
            this.subject = subject;
        }

        double getPercentage() {
            return CsvUtil.calculatePercentage(totalScore, totalMaxScore);
        }
    }

    /**
     * Helper class to track email sending results.
     */
    private record EmailResult(String studentName, String email, boolean success, String error) {
    }

    /**
     * Builds an error message from an exception.
     *
     * @param ex the exception
     * @return the error message
     */
    private String buildErrorMessage(Exception ex) {
        String errorMsg = ex.getMessage();
        if (errorMsg == null || errorMsg.isBlank()) {
            errorMsg = ex.getClass().getSimpleName();
        }
        return errorMsg;
    }

}

