package com.studentdetails.details.DTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

