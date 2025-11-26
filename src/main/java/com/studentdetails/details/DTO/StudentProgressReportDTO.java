package com.studentdetails.details.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProgressReportDTO {
    private Long studentId;
    private String studentName;
    private String branch;
    private List<StudentCourseSummaryDTO> courses;
    private List<StudentSubjectAnalyticsDTO> subjects;
    private long totalAssessments;
    private Double overallAverageScore;
    private Double overallPercentage;
    private LocalDate lastAssessmentDate;
}

