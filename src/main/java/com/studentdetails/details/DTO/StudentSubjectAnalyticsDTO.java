package com.studentdetails.details.DTO;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSubjectAnalyticsDTO {
    private String subject;
    private long assessments;
    private double totalScore;
    private double totalMaxScore;
    private Double averageScore;
    private Double percentage;
}

