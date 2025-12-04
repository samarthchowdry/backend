package com.studentdetails.details.Service;
import com.studentdetails.details.DTO.CourseDTO;
import java.util.List;
public interface CourseService {
    List<CourseDTO> getAllCourses();
    CourseDTO getCourseById(Long id);
    CourseDTO createCourse(CourseDTO courseDTO);
    CourseDTO updateCourse(Long id, CourseDTO courseDTO);
    void deleteCourse(Long id);
    long countCourses();
    List<CourseDTO> getFilteredCourses(String name, String code);
    List<CourseDTO> getCoursesByStudent(Long studentId);
    CourseDTO addStudentToCourse(Long courseId, Long studentId);
    CourseDTO removeStudentFromCourse(Long courseId, Long studentId);
    CourseDTO createCourseWithAuth(CourseDTO courseDTO, String roleHeader);
    CourseDTO updateCourseWithAuth(Long id, CourseDTO courseDTO, String roleHeader);
    CourseDTO addStudentToCourseWithAuth(Long courseId, Long studentId, String roleHeader);
    void deleteCourseWithAuth(Long id, String roleHeader);
    CourseDTO removeStudentFromCourseWithAuth(Long courseId, Long studentId, String roleHeader);
}

