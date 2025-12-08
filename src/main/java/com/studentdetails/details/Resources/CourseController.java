package com.studentdetails.details.Resources;

import com.studentdetails.details.DTO.CourseDTO;
import com.studentdetails.details.Exception.CourseNotFoundException;
import com.studentdetails.details.Repository.CourseRepository;
import com.studentdetails.details.Service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for course-related endpoints.
 * This class is used by Spring Framework for REST API handling.
 */
@RestController
@RequestMapping({"/api/courses", "/courses"})
@RequiredArgsConstructor
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class CourseController {

    private final CourseService courseService;
    private final CourseRepository courseRepository;

    /**
     * Counts the total number of courses.
     *
     * @return the total count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getCoursesCount() {
        long count = courseRepository.count();
        return ResponseEntity.ok(count);
    }

    /**
     * Retrieves all courses.
     * Supports optional filtering by name and code via query parameters.
     *
     * @param name optional course name filter
     * @param code optional course code filter
     * @return list of course DTOs
     */
    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses(
            @RequestParam(required = false) final String name,
            @RequestParam(required = false) final String code) {
        List<CourseDTO> allCourses = courseService.getAllCourses();
        
        // Apply filters if provided
        if ((name != null && !name.isBlank()) || (code != null && !code.isBlank())) {
            allCourses = allCourses.stream()
                    .filter(course -> {
                        boolean nameMatch = name == null || name.isBlank() || 
                                (course.getName() != null && course.getName().toLowerCase().contains(name.toLowerCase()));
                        boolean codeMatch = code == null || code.isBlank() || 
                                (course.getCode() != null && course.getCode().toLowerCase().contains(code.toLowerCase()));
                        return nameMatch && codeMatch;
                    })
                    .toList();
        }
        
        return ResponseEntity.ok(allCourses);
    }

    /**
     * Retrieves a course by ID.
     *
     * @param id the course ID
     * @return the course DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourseDTO> getCourseById(@PathVariable final Long id) {
        CourseDTO course = courseService.getCourseById(id);
        return ResponseEntity.ok(course);
    }

    /**
     * Exception handler for CourseNotFoundException.
     *
     * @param ex the exception
     * @return not found response
     */
    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<String> handleCourseNotFound(final CourseNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}

