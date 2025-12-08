package com.studentdetails.details.Security;

import com.studentdetails.details.Domain.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Optional;

/**
 * Utility class for accessing security context information.
 * Provides helper methods to get current user information from Spring Security context.
 */
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class SecurityUtils {

    /**
     * Gets the current authenticated user's email.
     *
     * @return the email or empty if not authenticated
     */
    public static Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return Optional.ofNullable(authentication.getName());
        }
        return Optional.empty();
    }

    /**
     * Gets the current authenticated user's role.
     *
     * @return the role or empty if not authenticated
     */
    public static Optional<UserRole> getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            if (authorities != null && !authorities.isEmpty()) {
                String authority = authorities.iterator().next().getAuthority();
                if (authority.startsWith("ROLE_")) {
                    String roleName = authority.substring(5); // Remove "ROLE_" prefix
                    try {
                        return Optional.of(UserRole.valueOf(roleName));
                    } catch (IllegalArgumentException _) {
                        // Invalid role name
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if the current user has a specific role.
     *
     * @param role the role to check
     * @return true if user has the role, false otherwise
     */
    public static boolean hasRole(UserRole role) {
        return getCurrentUserRole()
                .map(userRole -> userRole == role)
                .orElse(false);
    }

    /**
     * Checks if the current user is an admin.
     *
     * @return true if user is admin, false otherwise
     */
    public static boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    /**
     * Checks if the current user is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}






