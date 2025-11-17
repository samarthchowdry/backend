package com.studentdetails.details.Domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "student_marks")
public class StudentMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "subject", nullable = false, length = 128)
    private String subject;

    @Column(name = "assessment_name", length = 128)
    private String assessmentName;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "max_score")
    private Double maxScore;

    @Column(name = "grade", length = 16)
    private String grade;

    @Column(name = "assessed_on")
    private LocalDate assessedOn;

    @Column(name = "recorded_by", length = 128)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
        if (assessedOn == null) {
            assessedOn = recordedAt.toLocalDate();
        }
    }
}

