package com.studentdetails.details.Service;

import com.studentdetails.details.DTO.IndividualEmailResponse;

public interface AdminCommunicationService {
    int sendBroadcastEmail(String subject, String message);

    IndividualEmailResponse sendEmailToStudent(Long studentId, String subject, String message);
}


