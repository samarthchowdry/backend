package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.DTO.IndividualEmailResponse;
import com.studentdetails.details.Exception.StudentNotFoundException;
import com.studentdetails.details.Repository.StudentRepository;
import com.studentdetails.details.Service.AdminCommunicationService;
import com.studentdetails.details.Service.EmailService;
import com.studentdetails.details.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminCommunicationServiceImpl implements AdminCommunicationService {

    private final StudentRepository studentRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    public int sendBroadcastEmail(String subject, String message) {
        var emails = studentRepository.findAll().stream()
                .map(student -> student.getEmail())
                .filter(email -> email != null && !email.isBlank())
                .toList();

        emails.forEach(email -> emailService.sendEmail(email, subject, message));
        return emails.size();
    }

    @Override
    public IndividualEmailResponse sendEmailToStudent(Long studentId, String subject, String message) {
        var student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        var email = student.getEmail();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student does not have a registered email address");
        }

        emailService.sendEmail(email, subject, message);
        notificationService.createNotification(
                "Individual email sent",
                String.format("Email to %s (%s): %s", student.getName(), email, subject)
        );
        return new IndividualEmailResponse(student.getId(), student.getName(), email, subject);
    }
}


