package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {
    List<StudentMark> findByStudentIdOrderByAssessedOnDesc(Long studentId);

    java.util.Optional<StudentMark> findByIdAndStudentId(Long id, Long studentId);


// Batch fetch marks for multiple students in a single query.
// This eliminates N+1 query problem when processing multiple students.

    @org.springframework.data.jpa.repository.Query("SELECT m FROM StudentMark m WHERE m.student.id IN :studentIds ORDER BY m.student.id, m.assessedOn DESC")
    List<StudentMark> findByStudentIdInOrderByStudentIdAndAssessedOnDesc(@org.springframework.data.repository.query.Param("studentIds") java.util.List<Long> studentIds);
}

