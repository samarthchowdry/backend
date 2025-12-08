package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByEmail(String email);
    
    Optional<Teacher> findByGoogleSub(String googleSub);
    
    boolean existsByEmail(String email);
    
    boolean existsByGoogleSub(String googleSub);
}

