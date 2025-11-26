package com.studentdetails.details.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IndividualEmailRequest(
        @NotNull(message = "Student id is required")
        Long studentId,
        @NotBlank(message = "Subject is required")
        String subject,
        @NotBlank(message = "Message is required")
        String message
) {
}

