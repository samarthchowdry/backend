package com.studentdetails.details.Domain;
import jakarta.persistence.*;
import lombok.*;
import org.antlr.v4.runtime.misc.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name= "name")
    private String name;

    @NotNull
    @Column (name= "dob")
    private LocalDate dob;

    @NotNull
    @Column(name="email")
    private String email;

    @NotNull
    @Column(name="address")
    private String address;

    @NotNull
    @Column(name="branch")
    private String branch;

    // Many-to-Many: Many students can enroll in many courses
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "student_courses",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private
    List<Course> courses = new ArrayList<>();
}
