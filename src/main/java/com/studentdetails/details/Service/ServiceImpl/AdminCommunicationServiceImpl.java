package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.DTO.IndividualEmailResponse;
import com.studentdetails.details.Domain.BroadcastEmailTemplate;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Exception.StudentNotFoundException;
import com.studentdetails.details.Repository.BroadcastEmailTemplateRepository;
import com.studentdetails.details.Repository.StudentRepository;
import com.studentdetails.details.Service.AdminCommunicationService;
import com.studentdetails.details.Service.EmailService;
import com.studentdetails.details.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCommunicationServiceImpl implements AdminCommunicationService {

    private final StudentRepository studentRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
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

        int sentCount = 0;
        for (Student student : students) {
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
                sentCount++;
            } catch (Exception e) {
                log.error("Failed to send broadcast email to student {} ({}): {}", 
                    student.getName(), student.getEmail(), e.getMessage(), e);
            }
        }
        
        log.info("Broadcast email sent to {} students using template id={}", sentCount, template.getId());
        return sentCount;
    }

    @Override
    public IndividualEmailResponse sendEmailToStudent(Long studentId, String subject, String message) {
        var student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        var email = student.getEmail();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student does not have a registered email address");
        }

        // Send email using Thymeleaf template
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("subject", subject);
        templateVariables.put("name", student.getName());
        templateVariables.put("message", message);
        templateVariables.put("studentId", student.getId());
        templateVariables.put("branch", student.getBranch());
        
        emailService.sendEmailWithTemplate(email, subject, "email-template", templateVariables);
        
        notificationService.createNotification(
                "Individual email sent",
                String.format("Email to %s (%s): %s", student.getName(), email, subject)
        );
        return new IndividualEmailResponse(student.getId(), student.getName(), email, subject);
    }
}


