package com.studentdetails.details.Domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a log entry for daily report generation.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "daily_report_logs")
public class DailyReportLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;
    @Column(name = "file_name", nullable = false)
    private String fileName;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status;
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public enum ReportStatus {
        GENERATED,
        SENT,
        FAILED
    }
}


