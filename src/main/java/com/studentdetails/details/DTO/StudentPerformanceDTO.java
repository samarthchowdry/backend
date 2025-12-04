package com.studentdetails.details.DTO;
import lombok.Getter;
import lombok.ToString;
import java.time.LocalDate;
@Getter
@ToString
public class StudentPerformanceDTO {
    private Long studentId;
    private String studentName;
    private String branch;
    private long totalAssessments;
    private double totalScore;
    private double totalMaxScore;
    private Double averageScore;
    private Double percentage;
    private LocalDate lastAssessedOn;
    public StudentPerformanceDTO(Long studentId,
                                 String studentName,
                                 String branch,
                                 Long totalAssessments,
                                 Double totalScore,
                                 Double totalMaxScore,
                                 LocalDate lastAssessedOn) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.branch = branch;
        this.totalAssessments = totalAssessments == null ? 0L : totalAssessments;
        this.totalScore = totalScore == null ? 0.0 : totalScore;
        this.totalMaxScore = totalMaxScore == null ? 0.0 : totalMaxScore;
        this.lastAssessedOn = lastAssessedOn;
        if (this.totalAssessments > 0) {
            this.averageScore = this.totalScore / this.totalAssessments;
        }
        if (this.totalMaxScore > 0) {
            this.percentage = (this.totalScore / this.totalMaxScore) * 100.0;
        }
    }
}


