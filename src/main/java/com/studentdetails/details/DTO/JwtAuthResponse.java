package com.studentdetails.details.DTO;

import com.studentdetails.details.Domain.UserRole;

/**
 * JWT authentication response DTO.
 * Contains the JWT token and user information after successful authentication.
 */
public record JwtAuthResponse(
        String token,
        String email,
        String name,
        UserRole role,
        Long userId,
        String pictureUrl
) {
}







