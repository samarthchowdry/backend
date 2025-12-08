package com.studentdetails.details.Domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple single-row configuration for the daily report schedule.
 * Stores the hour and minute of day at which the report should run.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "report_schedule_config")
public class ReportScheduleConfig {

    private static final int DEFAULT_REPORT_HOUR = 10;
    private static final int DEFAULT_REPORT_MINUTE = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Hour of day in 24h format (0-23) when the daily report should be generated.
     */
    @Column(name = "report_hour", nullable = false)
    private int reportHour = DEFAULT_REPORT_HOUR;

    /**
     * Minute of hour (0-59) when the daily report should be generated.
     */
    @Column(name = "report_minute", nullable = false)
    private int reportMinute = DEFAULT_REPORT_MINUTE;
}


