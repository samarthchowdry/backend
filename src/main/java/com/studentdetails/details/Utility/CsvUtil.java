package com.studentdetails.details.Utility;

import com.studentdetails.details.Domain.LoginInfo;
import com.studentdetails.details.Domain.UserRole;
import com.studentdetails.details.Repository.LoginInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Utility class for CSV operations and common report utilities to avoid code duplication across report generators.
 */
public class CsvUtil {

    private static final Logger log = LoggerFactory.getLogger(CsvUtil.class);

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CsvUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Escapes a CSV value by wrapping it in quotes if it contains special characters.
     * Handles null values by returning an empty string.
     *
     * @param value the value to escape
     * @return the escaped CSV value
     */
    public static String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Safely gets a string value, returning empty string if null.
     *
     * @param value the value to get
     * @return the value or empty string if null
     */
    public static String safeString(String value) {
        return value != null ? value : "";
    }

    /**
     * Formats a double value to 2 decimal places.
     *
     * @param value the value to format
     * @return formatted string
     */
    public static String formatDouble(double value) {
        return String.format("%.2f", value);
    }

    /**
     * Formats a double value to 2 decimal places, handling null values.
     *
     * @param value the value to format (can be null)
     * @return formatted string or empty string if null
     */
    public static String formatDouble(Double value) {
        return value != null ? formatDouble(value.doubleValue()) : "";
    }

    /**
     * Calculates average score from total score and count.
     *
     * @param totalScore the total score
     * @param count      the number of assessments
     * @return average score, or 0.0 if count is 0
     */
    public static double calculateAverage(double totalScore, int count) {
        return count > 0 ? totalScore / count : 0.0;
    }

    /**
     * Calculates percentage from score and max score.
     *
     * @param score    the score
     * @param maxScore the maximum possible score
     * @return percentage, or 0.0 if maxScore is 0
     */
    public static double calculatePercentage(double score, double maxScore) {
        return maxScore > 0 ? (score / maxScore) * 100.0 : 0.0;
    }

    /**
     * Safely gets a value from an object, returning a default if null.
     *
     * @param value        the value to get
     * @param defaultValue the default value if null
     * @param <T>          the type of value
     * @return the value or default
     */
    public static <T> T safeValue(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Gets the email address of the most recently logged in admin user.
     * If no admin is found, returns null.
     * This method is shared across multiple report schedulers to avoid duplication.
     *
     * @param loginInfoRepository the repository to query admin users
     * @return admin email address or null if not found
     */
    public static String getAdminEmail(LoginInfoRepository loginInfoRepository) {
        try {
            Optional<LoginInfo> admin = loginInfoRepository.findFirstByRoleOrderByLastLoginAtDesc(UserRole.ADMIN);
            if (admin.isPresent()) {
                String email = admin.get().getEmail();
                log.info("Found admin email from database: {} (last login: {})",
                        email, admin.get().getLastLoginAt());
                return email;
            } else {
                log.warn("No admin user found in database. Checking all admin users...");
                List<LoginInfo> admins = loginInfoRepository.findByRole(UserRole.ADMIN);
                if (!admins.isEmpty()) {
                    String email = admins.getFirst().getEmail();
                    log.info("Found admin email from database: {}", email);
                    return email;
                } else {
                    log.error("No admin users found in database. Reports cannot be sent.");
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving admin email from database: {}", e.getMessage(), e);
            return null;
        }
    }

}

