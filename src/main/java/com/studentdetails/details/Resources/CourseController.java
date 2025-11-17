package com.studentdetails.details.Resources;

import com.studentdetails.details.DTO.CourseDTO;
import com.studentdetails.details.Service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "code", required = false) String code
    ) {
        return ResponseEntity.ok(courseService.getFilteredCourses(name, code));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDTO> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCourseCount() {
        return ResponseEntity.ok(courseService.countCourses());
    }

    @PostMapping
    public ResponseEntity<CourseDTO> createCourse(
            @RequestBody CourseDTO courseDTO,
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        return ResponseEntity.ok(courseService.createCourseWithAuth(courseDTO, roleHeader));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDTO> updateCourse(
            @PathVariable Long id,
            @RequestBody CourseDTO courseDTO,
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        return ResponseEntity.ok(courseService.updateCourseWithAuth(id, courseDTO, roleHeader));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCourse(
            @PathVariable Long id,
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        courseService.deleteCourseWithAuth(id, roleHeader);
        return ResponseEntity.ok("Course with ID " + id + " deleted successfully");
    }

    @PostMapping("/{courseId}/students/{studentId}")
    public ResponseEntity<CourseDTO> addStudentToCourse(
            @PathVariable Long courseId,
            @PathVariable Long studentId,
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        return ResponseEntity.ok(courseService.addStudentToCourseWithAuth(courseId, studentId, roleHeader));
    }

    @DeleteMapping("/{courseId}/students/{studentId}")
    public ResponseEntity<CourseDTO> removeStudentFromCourse(
            @PathVariable Long courseId,
            @PathVariable Long studentId,
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        return ResponseEntity.ok(courseService.removeStudentFromCourseWithAuth(courseId, studentId, roleHeader));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<CourseDTO>> getCoursesByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(courseService.getCoursesByStudent(studentId));
    }
}
