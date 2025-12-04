package com.studentdetails.details.Resources;

import com.studentdetails.details.Domain.UserRole;
import com.studentdetails.details.DTO.StudentProgressReportResponseDTO;
import com.studentdetails.details.Service.ReportService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final JavaMailSender mailSender;

    @Value("${app.report.admin-email:}")
    private String adminEmail;

    public ReportController(ReportService reportService, JavaMailSender mailSender) {
        this.reportService = reportService;
        this.mailSender = mailSender;
    }

    @GetMapping("/student-progress")
    public ResponseEntity<StudentProgressReportResponseDTO> generateStudentProgressReport(
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        UserRole role = resolveRole(roleHeader);
        enforceAdminRole(role);
        return ResponseEntity.ok(reportService.generateStudentProgressReport());
    }

    @PostMapping("/student-progress/email")
    public ResponseEntity<String> emailStudentProgressReport(
            @RequestBody StudentProgressReportResponseDTO report,
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        UserRole role = resolveRole(roleHeader);
        enforceAdminRole(role);

        if (adminEmail == null || adminEmail.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Admin email not configured (property 'app.report.admin-email').");
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Build Excel workbook from the report data (same columns as CSV)
            Sheet sheet = workbook.createSheet("Student Progress");
            int rowIdx = 0;

            Row header = sheet.createRow(rowIdx++);
            String[] columns = new String[]{
                    "Student Name",
                    "Branch",
                    "Total Assessments",
                    "Average Score",
                    "Overall Percentage",
                    "Last Assessment Date"
            };
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
            }

            if (report.getStudents() != null) {
                for (var student : report.getStudents()) {
                    Row row = sheet.createRow(rowIdx++);
                    int col = 0;
                    row.createCell(col++).setCellValue(student.getStudentName() != null ? student.getStudentName() : "");
                    row.createCell(col++).setCellValue(student.getBranch() != null ? student.getBranch() : "");
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
                    row.createCell(col).setCellValue(
                            student.getLastAssessmentDate() != null
                                    ? student.getLastAssessmentDate().toString()
                                    : ""
                    );
                }
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);

            String fileName = "student-progress-report-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")) + ".xlsx";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(adminEmail);
            helper.setSubject("Student Progress Report - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            helper.setText("Please find attached the student progress report generated from the system.", false);
            helper.addAttachment(fileName, () -> new java.io.ByteArrayInputStream(out.toByteArray()));

            mailSender.send(message);

            return ResponseEntity.ok("Report successfully emailed to " + adminEmail);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send report email: " + e.getMessage());
        }
    }

    private UserRole resolveRole(String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing role information");
        }
        try {
            return UserRole.valueOf(roleHeader.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid role value");
        }
    }

    private void enforceAdminRole(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin privileges required");
        }
    }
}

