package com.studentdetails.details.Service;
//email service
import com.studentdetails.details.Domain.EmailNotification;

import java.util.Map;
import java.util.List;

public interface EmailService {
    /**
     * Queue an email to be sent asynchronously by the scheduler.
     */
    void sendEmail(String toEmail, String subject, String body);

    /**
     * Queue an email using a Thymeleaf template to be sent asynchronously by the scheduler.
     * @param toEmail recipient email address
     * @param subject email subject
     * @param templateName name of the Thymeleaf template (without .html extension)
     * @param templateVariables variables to be used in the template
     */
    void sendEmailWithTemplate(String toEmail, String subject, String templateName, Map<String, Object> templateVariables);

    /**
     * Process pending/failed emails. Intended to be invoked by a scheduled job.
     */
    void processPendingEmails();

    List<EmailNotification> getAllEmailNotifications();

    void clearAllEmailNotifications();
}

