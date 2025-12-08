package com.studentdetails.details.DTO;

import lombok.*;

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

