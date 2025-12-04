package com.studentdetails.details.Domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple single-row configuration for the daily report schedule.
 * Stores the hour of day (0-23) at which the report should run.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "report_schedule_config")
public class ReportScheduleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Hour of day in 24h format (0-23) when the daily report should be generated.
     */
    @Column(name = "report_hour", nullable = false)
    private int reportHour = 10;

    /**
     * Minute of hour (0-59) when the daily report should be generated.
     */
    @Column(name = "report_minute", nullable = false)
    private int reportMinute = 30;
}


