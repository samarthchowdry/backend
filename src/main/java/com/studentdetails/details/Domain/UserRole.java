package com.studentdetails.details.Domain;

/**
 * Enumeration representing user roles in the system.
 */
public enum UserRole {
    /**
     * Administrator role with full access.
     */
    ADMIN,
    /**
     * Teacher role with limited administrative access.
     */
    TEACHER,
    /**
     * Student role with read-only access to own data.
     */
    STUDENT
}

