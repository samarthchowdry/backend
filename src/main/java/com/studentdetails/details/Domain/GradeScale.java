package com.studentdetails.details.Domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing grade scale configuration for grade calculation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "grade_scale")
public class GradeScale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "grade", nullable = false, length = 8, unique = true)
    private String grade;
    @Column(name = "min_percentage", nullable = false)
    private Double minPercentage;
    @Column(name = "max_percentage")
    private Double maxPercentage;
}

