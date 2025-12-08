package com.studentdetails.details.Domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a student in the system.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"courses", "marks"})
@Entity
@Table(name = "students")
public class Student {
    @Id
    @EqualsAndHashCode.Include
    private Long id;
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;
    @NotNull
    @Column(name = "dob", nullable = false)
    private LocalDate dob;
    @NotNull
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @NotNull
    @Column(name = "address", nullable = false)
    private String address;
    @NotNull
    @Column(name = "branch", nullable = false)
    private String branch;
    /**
     * Hashed password for student login.
     * Passwords are hashed using BCrypt before storage.
     */
    @Column(name = "password", length = 255)
    private String password;
    // Many-to-Many: Many students can enroll in many courses
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "student_courses",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses = new ArrayList<>();
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StudentMark> marks = new HashSet<>();
}
