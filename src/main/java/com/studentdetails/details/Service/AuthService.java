package com.studentdetails.details.Service;

import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {
    /**
     * Verifies a Google ID token and returns user information.
     *
     * @param body request body containing the idToken
     * @return response entity with user information
     */
    ResponseEntity<Object> verifyGoogleToken(Map<String, String> body);

    /**
     * Completes Google signup process and creates/updates user account.
     *
     * @param body request body containing user information
     * @return response entity with user information
     */
    ResponseEntity<Object> completeGoogleSignup(Map<String, String> body);

    /**
     * Updates user role (admin only).
     *
     * @param roleHeader the role header for authentication (optional, will try to extract from JWT if not provided)
     * @param authorizationHeader the Authorization header containing JWT token (optional)
     * @param body request body containing role and user information
     * @return response entity with updated user information
     */
    ResponseEntity<Object> updateUserRole(String roleHeader, String authorizationHeader, Map<String, String> body);

    /**
     * Unified login endpoint for all user types (Student, Teacher, Admin) using Google Sign-In.
     * This is the single URL for all login operations.
     *
     * @param body request body containing the idToken from Google
     * @return response entity with JWT token and user information
     */
    ResponseEntity<Object> login(Map<String, String> body);

    /**
     * Login with username/email and password for all user types (Student, Teacher, Admin).
     *
     * @param body request body containing username/email and password
     * @return response entity with JWT token and user information
     */
    ResponseEntity<Object> loginWithCredentials(Map<String, String> body);

    /**
     * Creates a new user (teacher or admin) - admin only.
     *
     * @param body request body containing user information (email, password, role, fullName)
     * @return response entity with created user information
     */
    ResponseEntity<Object> createUser(Map<String, String> body);
}
