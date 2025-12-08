package com.studentdetails.details.Domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a course in the system.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "students")
@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;
    @NotNull
    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;
    @Column(name = "description", length = 512)
    private String description;
    @Column(name = "credits")
    private Integer credits;
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "courses", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Student> students = new ArrayList<>();
}