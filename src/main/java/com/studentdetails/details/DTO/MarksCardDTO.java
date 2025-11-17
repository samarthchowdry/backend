package com.studentdetails.details.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarksCardDTO {
    private Long studentId;
    private String studentName;
    private String branch;
    private String email;
    private LocalDate dateOfBirth;
    private List<String> courses;
    private List<StudentMarkDTO> marks;
    private Double totalScore;
    private Double totalMaxScore;
    private Double percentage;
    private String overallGrade;
    private LocalDate generatedOn;
}

