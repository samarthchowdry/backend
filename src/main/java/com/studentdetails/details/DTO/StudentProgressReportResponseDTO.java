package com.studentdetails.details.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProgressReportResponseDTO {
    private Instant generatedAt;
    private long totalStudents;
    private long totalAssessments;
    private List<StudentProgressReportDTO> students;
}

