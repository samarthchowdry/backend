package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.Domain.Notification;
import com.studentdetails.details.Repository.NotificationRepository;
import com.studentdetails.details.Service.NotificationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service implementation for notification operations.
 * This class is used by Spring Framework for dependency injection.
 */
@AllArgsConstructor
@Service
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void createNotification(String title, String message) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setStatus(Notification.NotificationStatus.UNREAD);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));
        notification.setStatus(Notification.NotificationStatus.READ);
        return notificationRepository.save(notification);
    }

    @Override
    public void clearAllNotifications() {
        notificationRepository.deleteAll();
    }
}

