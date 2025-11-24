package com.studentdetails.details.Service;

import com.studentdetails.details.Domain.Notification;
import java.util.List;

public interface NotificationService {
    void createNotification(String title, String message);
    List<Notification> getAllNotifications();
    Notification markAsRead(Long notificationId);
    void clearAllNotifications();
}

