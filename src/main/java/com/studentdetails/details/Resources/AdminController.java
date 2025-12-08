package com.studentdetails.details.Resources;

import com.studentdetails.details.DTO.BroadcastEmailRequest;
import com.studentdetails.details.DTO.BroadcastEmailResponse;
import com.studentdetails.details.DTO.IndividualEmailRequest;
import com.studentdetails.details.DTO.IndividualEmailResponse;
import com.studentdetails.details.Domain.EmailNotification;
import com.studentdetails.details.Security.SecurityUtils;
import com.studentdetails.details.Service.AdminCommunicationService;
import com.studentdetails.details.Repository.LoginInfoRepository;
import com.studentdetails.details.Repository.TeacherRepository;
import com.studentdetails.details.Service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * REST controller for admin operations including email management.
 * This class is used by Spring Framework for HTTP request mapping.
 * Methods are invoked via HTTP requests, not direct method calls.
 */
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/admin")
@SuppressWarnings({"java:S1118", "unused"}) // Suppress unused warnings - class is used by Spring Framework
public class AdminController {

    private final EmailService emailService;
    private final AdminCommunicationService adminCommunicationService;
    private final TeacherRepository teacherRepository;
    private final LoginInfoRepository loginInfoRepository;

    /**
     * Constructor for AdminController.
     * This constructor is used by Spring Framework for dependency injection.
     *
     * @param emailService the email service
     * @param adminCommunicationService the admin communication service
     */
    @SuppressWarnings("unused") // Suppress unused warning - constructor is used by Spring Framework
    public AdminController(final EmailService emailService,
                           final AdminCommunicationService adminCommunicationService,
                           final TeacherRepository teacherRepository,
                           final LoginInfoRepository loginInfoRepository) {
        this.emailService = emailService;
        this.adminCommunicationService = adminCommunicationService;
        this.teacherRepository = teacherRepository;
        this.loginInfoRepository = loginInfoRepository;
    }

    /**
     * Retrieves email notification status (admin only).
     * This method is called via HTTP GET request to /api/admin/email-status.
     * Production-ready: Uses Spring Security for authentication.
     *
     * @return response entity with list of email notifications
     */
    @GetMapping("/email-status")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings("unused") // Suppress unused warning - method is called via HTTP
    public ResponseEntity<List<EmailNotification>> getEmailStatus() {
        return ResponseEntity.ok(emailService.getAllEmailNotifications());
    }

    /**
     * Clears all email notifications (admin only).
     * This method is called via HTTP DELETE request to /api/admin/email-status.
     * Production-ready: Uses Spring Security for authentication.
     *
     * @return no content response
     */
    @DeleteMapping("/email-status")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings("unused") // Suppress unused warning - method is called via HTTP
    public ResponseEntity<Void> clearEmailStatus() {
        emailService.clearAllEmailNotifications();
        return ResponseEntity.noContent().build();
    }

    /**
     * Sends a broadcast email to all students (admin only).
     * This method is called via HTTP POST request to /api/admin/email-broadcast.
     * Production-ready: Uses Spring Security for authentication.
     *
     * @param request the broadcast email request
     * @return response entity with broadcast email response
     */
    @PostMapping("/email-broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings("unused") // Suppress unused warning - method is called via HTTP
    public ResponseEntity<BroadcastEmailResponse> sendBroadcastEmail(
            @Valid @RequestBody final BroadcastEmailRequest request) {
        int recipients = adminCommunicationService.sendBroadcastEmail(
                request.subject(),
                request.message()
        );

        return ResponseEntity.ok(new BroadcastEmailResponse(recipients, request.subject()));
    }

    /**
     * Sends an email to a specific student (admin only).
     * This method is called via HTTP POST request to /api/admin/email-student.
     * Production-ready: Uses Spring Security for authentication.
     *
     * @param request the individual email request
     * @return response entity with individual email response
     */
    @PostMapping("/email-student")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings("unused") // Suppress unused warning - method is called via HTTP
    public ResponseEntity<IndividualEmailResponse> sendEmailToStudent(
            @Valid @RequestBody final IndividualEmailRequest request) {
        IndividualEmailResponse response = adminCommunicationService.sendEmailToStudent(
                request.studentId(),
                request.subject(),
                request.message()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a teacher by email (admin only).
     * Removes both the teacher record and the associated login_info entry (if present).
     */
    @DeleteMapping("/teachers/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteTeacher(
            @PathVariable final String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        String normalizedEmail = email.trim().toLowerCase();

        var teacher = teacherRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        // Delete teacher record
        teacherRepository.delete(teacher);

        // Also delete login_info linked to this email (if exists)
        loginInfoRepository.findByEmail(normalizedEmail)
                .ifPresent(loginInfoRepository::delete);

        return ResponseEntity.ok(Map.of(
                "message", "Teacher deleted successfully",
                "email", normalizedEmail
        ));
    }
}
