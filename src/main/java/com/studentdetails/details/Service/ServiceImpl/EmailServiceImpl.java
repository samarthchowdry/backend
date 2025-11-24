package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.Domain.EmailNotification;
import com.studentdetails.details.Repository.EmailNotificationRepository;
import com.studentdetails.details.Service.EmailService;
import lombok.AllArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailNotificationRepository emailNotificationRepository;

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        // Create email notification record with PENDING status
        EmailNotification emailNotification = new EmailNotification();
        emailNotification.setToEmail(toEmail);
        emailNotification.setSubject(subject);
        emailNotification.setStatus(EmailNotification.EmailStatus.PENDING);
        emailNotification.setSentTime(null);
        emailNotification = emailNotificationRepository.save(emailNotification);

        try {
            // Send email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            // Update status to SENT
            emailNotification.setStatus(EmailNotification.EmailStatus.SENT);
            emailNotification.setSentTime(LocalDateTime.now());
            emailNotificationRepository.save(emailNotification);
        } catch (Exception e) {
            // Update status to FAILED
            emailNotification.setStatus(EmailNotification.EmailStatus.FAILED);
            emailNotification.setSentTime(LocalDateTime.now());
            emailNotificationRepository.save(emailNotification);
            // Log the error but don't throw to avoid breaking the main flow
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Override
    public List<EmailNotification> getAllEmailNotifications() {
        return emailNotificationRepository.findAllByOrderBySentTimeDesc();
    }

    @Override
    public void clearAllEmailNotifications() {
        emailNotificationRepository.deleteAll();
    }
}

