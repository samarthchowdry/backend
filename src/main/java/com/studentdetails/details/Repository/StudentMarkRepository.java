package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {
    List<StudentMark> findByStudentIdOrderByAssessedOnDesc(Long studentId);

    java.util.Optional<StudentMark> findByIdAndStudentId(Long id, Long studentId);
}

