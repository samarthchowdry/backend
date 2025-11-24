package com.studentdetails.details.DTO;

import jakarta.validation.constraints.NotBlank;

public record BroadcastEmailRequest(
        @NotBlank(message = "Subject is required")
        String subject,
        @NotBlank(message = "Message is required")
        String message
) {}


