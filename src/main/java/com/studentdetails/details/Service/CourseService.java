package com.studentdetails.details.Service;

import com.studentdetails.details.DTO.CourseDTO;

import java.util.List;

/**
 * Service interface for course-related operations.
 */
public interface CourseService {

    /**
     * Retrieves all courses.
     *
     * @return list of all course DTOs
     */
    List<CourseDTO> getAllCourses();

    /**
     * Retrieves a course by ID.
     *
     * @param id the course ID
     * @return the course DTO
     */
    CourseDTO getCourseById(Long id);

    CourseDTO createCourse(CourseDTO courseDTO);

    CourseDTO updateCourse(Long id, CourseDTO courseDTO);

    void deleteCourse(Long id);

    CourseDTO addStudentToCourse(Long courseId, Long studentId);

    CourseDTO removeStudentFromCourse(Long courseId, Long studentId);

    CourseDTO createCourseWithAuth(CourseDTO courseDTO, String roleHeader);

    CourseDTO updateCourseWithAuth(Long id, CourseDTO courseDTO, String roleHeader);

    CourseDTO addStudentToCourseWithAuth(Long courseId, Long studentId, String roleHeader);

    void deleteCourseWithAuth(Long id, String roleHeader);

    CourseDTO removeStudentFromCourseWithAuth(Long courseId, Long studentId, String roleHeader);
}

