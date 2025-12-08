package com.studentdetails.details.Resources;

import com.studentdetails.details.Service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Constructor for AuthController.
     *
     * @param authService the authentication service
     */
    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    /**
     * Unified login endpoint for all user types (Student, Teacher, Admin) using Google Sign-In.
     * This is the single URL for all login operations - students, teachers, and admins all use this endpoint.
     *
     * @param body request body containing the idToken from Google
     * @return response entity with JWT token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody final Map<String, String> body) {
        if (body == null) {
            return ResponseEntity.badRequest().build();
        }
        // Check if it's Google Sign-In (has idToken) or username/password login
        if (body.containsKey("idToken")) {
            return authService.login(body);
        } else if (body.containsKey("username") || body.containsKey("password")) {
            return authService.loginWithCredentials(body);
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Either idToken (for Google Sign-In) or username/password is required"));
        }
    }

    /**
     * Verifies a Google ID token.
     *
     * @param body request body containing the idToken
     * @return response entity with user information
     */
    @PostMapping("/google")
    public ResponseEntity<Object> verifyGoogleToken(@RequestBody final Map<String, String> body) {
        if (body == null) {
            return ResponseEntity.badRequest().build();
        }
        return authService.verifyGoogleToken(body);
    }

    /**
     * Completes Google signup process.
     *
     * @param body request body containing user information
     * @return response entity with user information
     */
    @PostMapping("/google/complete")
    public ResponseEntity<Object> completeGoogleSignup(@RequestBody final Map<String, String> body) {
        if (body == null) {
            return ResponseEntity.badRequest().build();
        }
        return authService.completeGoogleSignup(body);
    }

    /**
     * Updates user role (admin only).
     * Requires authentication via JWT token or X-Role header (for backward compatibility).
     *
     * @param roleHeader the role header for authentication (optional, will try to extract from JWT if not provided)
     * @param authorizationHeader the Authorization header containing JWT token
     * @param body request body containing role and user information
     * @return response entity with updated user information
     */
    @PatchMapping("/role")
    public ResponseEntity<Object> updateUserRole(
            @RequestHeader(name = "X-Role", required = false) final String roleHeader,
            @RequestHeader(name = "Authorization", required = false) final String authorizationHeader,
            @RequestBody final Map<String, String> body) {
        if (body == null) {
            return ResponseEntity.badRequest().build();
        }
        return authService.updateUserRole(roleHeader, authorizationHeader, body);
    }

    /**
     * Creates a new user (teacher or admin) - admin only.
     * Note: This endpoint is under /api/auth/admin/users but requires ADMIN role via @PreAuthorize.
     *
     * @param body request body containing user information (email, password, role, fullName)
     * @return response entity with created user information
     */
    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> createUser(@RequestBody final Map<String, String> body) {
        if (body == null) {
            return ResponseEntity.badRequest().build();
        }
        return authService.createUser(body);
    }
}
