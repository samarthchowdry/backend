package com.studentdetails.details.Repository;
import com.studentdetails.details.Domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
}
