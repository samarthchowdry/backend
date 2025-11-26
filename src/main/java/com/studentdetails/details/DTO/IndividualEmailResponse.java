package com.studentdetails.details.DTO;

public record IndividualEmailResponse(
        Long studentId,
        String studentName,
        String email,
        String subject
) {
}

