package com.studentdetails.details.Service;

/**
 * Service interface for admin communication operations.
 */
public interface AdminCommunicationService {
    /**
     * Sends a broadcast email to all students.
     *
     * @param subject the email subject
     * @param message the email message
     * @return the number of recipients
     */
    int sendBroadcastEmail(String subject, String message);

    /**
     * Sends an email to a specific student.
     *
     * @param studentId the student ID
     * @param subject the email subject
     * @param message the email message
     * @return the individual email response
     */
    com.studentdetails.details.DTO.IndividualEmailResponse sendEmailToStudent(
            Long studentId, String subject, String message);
}
