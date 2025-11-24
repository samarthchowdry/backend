package com.studentdetails.details.Service;

import com.studentdetails.details.Domain.EmailNotification;
import java.util.List;

public interface EmailService {
    void sendEmail(String toEmail, String subject, String body);
    List<EmailNotification> getAllEmailNotifications();
    void clearAllEmailNotifications();
}

