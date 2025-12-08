package com.studentdetails.details.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for StudentMark entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentMarkDTO {
    private Long id;
    private Long studentId;
    private String subject;
    private String assessmentName;
    private Double score;
    private Double maxScore;
    private String grade;
    private LocalDate assessedOn;
    private String recordedBy;
    private LocalDateTime recordedAt;
}

