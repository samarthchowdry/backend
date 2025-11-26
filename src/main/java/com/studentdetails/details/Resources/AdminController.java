package com.studentdetails.details.Resources;

import com.studentdetails.details.Domain.EmailNotification;
import com.studentdetails.details.Domain.Notification;
import com.studentdetails.details.Domain.UserRole;
import com.studentdetails.details.DTO.BroadcastEmailRequest;
import com.studentdetails.details.DTO.IndividualEmailRequest;
import com.studentdetails.details.DTO.IndividualEmailResponse;
import com.studentdetails.details.Service.AdminCommunicationService;
import com.studentdetails.details.Service.EmailService;
import com.studentdetails.details.Service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/admin")
public class AdminController {

    private final EmailService emailService;
    private final NotificationService notificationService;
    private final AdminCommunicationService adminCommunicationService;

    public AdminController(EmailService emailService,
                           NotificationService notificationService,
                           AdminCommunicationService adminCommunicationService) {
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.adminCommunicationService = adminCommunicationService;
    }

    @GetMapping("/email-status")
    public ResponseEntity<List<EmailNotification>> getEmailStatus(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @RequestParam(name = "role", required = false) String roleQueryParam) {
        UserRole role = resolveRole(roleHeader, roleQueryParam);
        enforceAdminRole(role);
        return ResponseEntity.ok(emailService.getAllEmailNotifications());
    }

    @DeleteMapping("/email-status")
    public ResponseEntity<Void> clearEmailStatus(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @RequestParam(name = "role", required = false) String roleQueryParam) {
        UserRole role = resolveRole(roleHeader, roleQueryParam);
        enforceAdminRole(role);
        emailService.clearAllEmailNotifications();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/in-app-notifications")
    public ResponseEntity<List<Notification>> getInAppNotifications(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @RequestParam(name = "role", required = false) String roleQueryParam) {
        UserRole role = resolveRole(roleHeader, roleQueryParam);
        enforceAdminRole(role);
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @PostMapping("/email-broadcast")
    public ResponseEntity<?> sendBroadcastEmail(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @RequestParam(name = "role", required = false) String roleQueryParam,
            @Valid @RequestBody BroadcastEmailRequest request) {
        UserRole role = resolveRole(roleHeader, roleQueryParam);
        enforceAdminRole(role);
        int recipients = adminCommunicationService.sendBroadcastEmail(request.subject(), request.message());
        return ResponseEntity.ok(java.util.Map.of(
                "recipients", recipients,
                "subject", request.subject()
        ));
    }

    @PostMapping("/email-student")
    public ResponseEntity<IndividualEmailResponse> sendEmailToStudent(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @RequestParam(name = "role", required = false) String roleQueryParam,
            @Valid @RequestBody IndividualEmailRequest request) {
        UserRole role = resolveRole(roleHeader, roleQueryParam);
        enforceAdminRole(role);
        IndividualEmailResponse response = adminCommunicationService.sendEmailToStudent(
                request.studentId(), request.subject(), request.message());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/in-app-notifications")
    public ResponseEntity<Void> clearInAppNotifications(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @RequestParam(name = "role", required = false) String roleQueryParam) {
        UserRole role = resolveRole(roleHeader, roleQueryParam);
        enforceAdminRole(role);
        notificationService.clearAllNotifications();
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/in-app-notifications/{id}/read")
    public ResponseEntity<Notification> markNotificationAsRead(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @RequestParam(name = "role", required = false) String roleQueryParam,
            @PathVariable Long id) {
        UserRole role = resolveRole(roleHeader, roleQueryParam);
        enforceAdminRole(role);
        Notification updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(updated);
    }

    private UserRole resolveRole(String roleHeader, String roleQueryParam) {
        String roleSource = (roleHeader != null && !roleHeader.isBlank())
                ? roleHeader
                : roleQueryParam;
        if (roleSource == null || roleSource.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing role information");
        }
        try {
            return UserRole.valueOf(roleSource.trim().toUpperCase());
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

