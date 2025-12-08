package com.studentdetails.details.Domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing an email notification in the queue.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "email_notifications")
public class EmailNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "to_email", nullable = false)
    private String toEmail;
    @Column(name = "subject", nullable = false)
    private String subject;
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;
    @Column(name = "is_html", nullable = false)
    private Boolean isHtml = false;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatus status;
    @Column(name = "sent_time")
    private LocalDateTime sentTime;
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    @Column(name = "last_attempt_time")
    private LocalDateTime lastAttemptTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public enum EmailStatus {
        PENDING,
        SENT,
        FAILED
    }
}

