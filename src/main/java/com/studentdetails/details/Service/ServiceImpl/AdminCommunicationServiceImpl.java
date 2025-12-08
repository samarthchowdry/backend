package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.Domain.BroadcastEmailTemplate;
import com.studentdetails.details.Repository.BroadcastEmailTemplateRepository;
import com.studentdetails.details.Repository.StudentRepository;
import com.studentdetails.details.Service.AdminCommunicationService;
import com.studentdetails.details.Service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCommunicationServiceImpl implements AdminCommunicationService {

    private final StudentRepository studentRepository;
    private final EmailService emailService;
    private final BroadcastEmailTemplateRepository broadcastEmailTemplateRepository;

    @Override
    @Transactional
    public int sendBroadcastEmail(String subject, String message) {
        // First, save the subject and message to the database
        BroadcastEmailTemplate template = new BroadcastEmailTemplate();
        template.setSubject(subject);
        template.setMessage(message);
        template.setCreatedAt(LocalDateTime.now());

        // Save to database - this ensures the message is persisted
        template = broadcastEmailTemplateRepository.save(template);

        // Log the save operation to confirm it was successful
        log.info("Broadcast email template saved to database with id={}, subject='{}', message length={}",
                template.getId(), template.getSubject(), template.getMessage().length());

        // Verify the save was successful by checking if ID was generated
        if (template.getId() == null) {
            log.error("Failed to save broadcast email template to database - ID is null");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save broadcast email template to database");
        }

        // Now send emails to all students using Thymeleaf template
        var students = studentRepository.findAll().stream()
                .filter(student -> student.getEmail() != null && !student.getEmail().isBlank())
                .toList();

        // OPTIMIZATION: Send emails asynchronously in parallel for better performance
        log.info("Sending broadcast emails to {} students in parallel...", students.size());

        int[] sentCount = {0}; // Use array to allow modification in lambda
        List<java.util.concurrent.CompletableFuture<Void>> futures = students.stream()
                .map(student -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        Map<String, Object> templateVariables = new HashMap<>();
                        templateVariables.put("subject", subject);
                        templateVariables.put("message", message);

                        emailService.sendEmailWithTemplate(
                                student.getEmail(),
                                subject,
                                "email-template",
                                templateVariables
                        );
                        synchronized (sentCount) {
                            sentCount[0]++;
                        }
                    } catch (Exception e) {
                        log.error("Failed to send broadcast email to student {} ({}): {}",
                                student.getName(), student.getEmail(), e.getMessage(), e);
                    }
                }))
                .toList();

        // Wait for all emails to be queued (they'll be sent asynchronously)
        java.util.concurrent.CompletableFuture<Void> allFutures = java.util.concurrent.CompletableFuture.allOf(
                futures.toArray(new java.util.concurrent.CompletableFuture[0]));
        allFutures.join();

        log.info("Queued {} broadcast emails for async processing", sentCount[0]);

        log.info("Broadcast email sent to {} students using template id={}", sentCount[0], template.getId());
        return sentCount[0];
    }

    @Override
    @Transactional
    public com.studentdetails.details.DTO.IndividualEmailResponse sendEmailToStudent(
            Long studentId, String subject, String message) {
        com.studentdetails.details.Domain.Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new com.studentdetails.details.Exception.StudentNotFoundException(studentId));

        if (student.getEmail() == null || student.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Student does not have a valid email address");
        }

        try {
            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("subject", subject);
            templateVariables.put("message", message);
            templateVariables.put("studentName", student.getName());

            emailService.sendEmailWithTemplate(
                    student.getEmail(),
                    subject,
                    "email-template",
                    templateVariables
            );

            log.info("Email queued for student {} ({})", student.getName(), student.getEmail());

            return new com.studentdetails.details.DTO.IndividualEmailResponse(
                    student.getId(),
                    student.getName(),
                    student.getEmail(),
                    subject
            );
        } catch (Exception e) {
            log.error("Failed to send email to student {} ({}): {}",
                    student.getName(), student.getEmail(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to send email: " + e.getMessage());
        }
    }
}

