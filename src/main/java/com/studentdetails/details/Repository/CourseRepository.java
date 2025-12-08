package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Override
    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.students")
    @NonNull
    List<Course> findAll();
}

