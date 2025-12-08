package com.studentdetails.details.Service;

import com.studentdetails.details.Domain.Notification;

import java.util.List;

/**
 * Service interface for notification operations.
 */
public interface NotificationService {
    /**
     * Creates a new notification.
     *
     * @param title the notification title
     * @param message the notification message
     */
    void createNotification(String title, String message);

    /**
     * Retrieves all notifications.
     *
     * @return list of all notifications
     */
    List<Notification> getAllNotifications();

    /**
     * Marks a notification as read.
     *
     * @param notificationId the notification ID
     * @return the updated notification
     */
    Notification markAsRead(Long notificationId);

    /**
     * Clears all notifications.
     */
    void clearAllNotifications();
}

