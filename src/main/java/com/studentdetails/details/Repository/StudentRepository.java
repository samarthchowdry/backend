package com.studentdetails.details.Repository;

import com.studentdetails.details.DTO.StudentPerformanceDTO;
import com.studentdetails.details.Domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.courses LEFT JOIN FETCH s.marks WHERE " +
            "(:id IS NULL OR s.id = :id) AND " +
            "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:email IS NULL OR LOWER(s.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:dob IS NULL OR s.dob = :dob) AND " +
            "(:branch IS NULL OR LOWER(s.branch) LIKE LOWER(CONCAT('%', :branch, '%')))")
    List<Student> findByFilters(@Param("name") String name,
                                @Param("dob") LocalDate dob,
                                @Param("email") String email,
                                @Param("branch") String branch,
                                @Param("id") Long id);

    @Override
    @Query("SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.courses LEFT JOIN FETCH s.marks")
    @NonNull
    List<Student> findAll();

    @Query("SELECT DISTINCT s FROM Student s LEFT JOIN FETCH s.courses LEFT JOIN FETCH s.marks WHERE s.id = :id")
    Optional<Student> findByIdWithCourses(@Param("id") Long id);

    @Query("SELECT COALESCE(MAX(s.id), 0) FROM Student s")
    Long findMaxId();

    @Query("""
            SELECT new com.studentdetails.details.DTO.StudentPerformanceDTO(
                s.id,
                s.name,
                s.branch,
                COUNT(m.id),
                COALESCE(SUM(m.score), 0),
                COALESCE(SUM(m.maxScore), 0),
                MAX(m.assessedOn)
            )
            FROM Student s
            LEFT JOIN s.marks m
            GROUP BY s.id, s.name, s.branch
            ORDER BY s.name ASC
            """)
    List<StudentPerformanceDTO> fetchPerformanceSummary();

    @Query("SELECT LOWER(TRIM(s.email)) FROM Student s WHERE s.email IS NOT NULL AND s.email != ''")
    java.util.Set<String> findAllEmailAddresses();
}
