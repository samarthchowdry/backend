package com.studentdetails.details.Resources;

import com.studentdetails.details.DTO.MarksCardDTO;
import com.studentdetails.details.DTO.StudentDTO;
import com.studentdetails.details.DTO.StudentMarkDTO;
import com.studentdetails.details.DTO.StudentPerformanceDTO;
import com.studentdetails.details.Exception.StudentNotFoundException;
import com.studentdetails.details.Service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for student-related endpoints.
 * This class is used by Spring Framework for REST API handling.
 */
@RestController
@RequestMapping({"/api/students", "/students"})
@RequiredArgsConstructor
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class StudentController {

    private final StudentService studentService;

    /**
     * Retrieves all students with optional filters.
     *
     * @param id the student ID filter
     * @param name the student name filter
     * @param dob the date of birth filter
     * @param email the email filter
     * @param address the address filter
     * @param branch the branch filter
     * @return list of student DTOs
     */
    @GetMapping
    @SuppressWarnings({"java:S1172", "unused"}) // Suppress unused parameter warnings - parameters are used via query params
    public ResponseEntity<List<StudentDTO>> getAllStudents(
            @RequestParam(required = false) final Long id,
            @RequestParam(required = false) final String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dob,
            @RequestParam(required = false) final String email,
            @RequestParam(required = false) final String address,
            @RequestParam(required = false) final String branch) {
        List<StudentDTO> students = studentService.getFilteredStudents(name, dob, email, branch, id);
        return ResponseEntity.ok(students);
    }

    /**
     * Retrieves a student by ID.
     *
     * @param id the student ID
     * @return the student DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable final Long id) {
        StudentDTO student = studentService.getStudentById(id);
        return ResponseEntity.ok(student);
    }

    /**
     * Creates a new student.
     *
     * @param studentDTO the student data
     * @return the created student DTO
     */
    @PostMapping
    public ResponseEntity<StudentDTO> createStudent(@RequestBody final StudentDTO studentDTO) {
        if (studentDTO == null) {
            return ResponseEntity.badRequest().build();
        }
        if (studentDTO.getPassword() != null && studentDTO.getPassword().trim().length() > 0
                && studentDTO.getPassword().trim().length() < 6) {
            return ResponseEntity.badRequest().body(null);
        }
        StudentDTO created = studentService.createStudent(studentDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing student.
     *
     * @param id the student ID
     * @param studentDTO the updated student data
     * @return the updated student DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> updateStudent(
            @PathVariable final Long id,
            @RequestBody final StudentDTO studentDTO) {
        if (studentDTO == null) {
            return ResponseEntity.badRequest().build();
        }
        StudentDTO updated = studentService.updateStudent(id, studentDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a student by ID.
     *
     * @param id the student ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable final Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Counts the total number of students.
     *
     * @return the total count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getStudentsCount() {
        long count = studentService.countStudents();
        return ResponseEntity.ok(count);
    }

    /**
     * Retrieves student performance summary.
     *
     * @return list of student performance DTOs
     */
    @GetMapping("/performance")
    public ResponseEntity<List<StudentPerformanceDTO>> getPerformanceSummary() {
        List<StudentPerformanceDTO> summary = studentService.getStudentPerformanceSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Bulk uploads students from a CSV file.
     *
     * @param file the CSV file
     * @return summary message
     */
    @PostMapping("/bulk-upload")
    public ResponseEntity<String> bulkUploadStudents(@RequestParam("file") final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required");
        }
        String result = studentService.bulkUploadStudents(file);
        return ResponseEntity.ok(result);
    }

    /**
     * Bulk updates students from a CSV file.
     *
     * @param file the CSV file
     * @return summary message
     */
    @PostMapping("/bulk-update")
    public ResponseEntity<String> bulkUpdateStudents(@RequestParam("file") final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required");
        }
        String result = studentService.bulkUpdateStudents(file);
        return ResponseEntity.ok(result);
    }

    /**
     * Adds a mark for a student.
     *
     * @param studentId the student ID
     * @param markDTO the mark data
     * @return the created mark DTO
     */
    @PostMapping("/{studentId}/marks")
    public ResponseEntity<StudentMarkDTO> addMark(
            @PathVariable final Long studentId,
            @RequestBody final StudentMarkDTO markDTO) {
        if (markDTO == null) {
            return ResponseEntity.badRequest().build();
        }
        StudentMarkDTO created = studentService.addMark(studentId, markDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates a mark for a student.
     *
     * @param studentId the student ID
     * @param markId the mark ID
     * @param markDTO the updated mark data
     * @return the updated mark DTO
     */
    @PutMapping("/{studentId}/marks/{markId}")
    public ResponseEntity<StudentMarkDTO> updateMark(
            @PathVariable final Long studentId,
            @PathVariable final Long markId,
            @RequestBody final StudentMarkDTO markDTO) {
        if (markDTO == null) {
            return ResponseEntity.badRequest().build();
        }
        StudentMarkDTO updated = studentService.updateMark(studentId, markId, markDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Retrieves all marks for a student.
     *
     * @param studentId the student ID
     * @return list of mark DTOs
     */
    @GetMapping("/{studentId}/marks")
    public ResponseEntity<List<StudentMarkDTO>> getMarks(@PathVariable final Long studentId) {
        List<StudentMarkDTO> marks = studentService.getMarks(studentId);
        return ResponseEntity.ok(marks);
    }

    /**
     * Retrieves a marks card for a student.
     *
     * @param studentId the student ID
     * @return the marks card DTO
     */
    @GetMapping("/{studentId}/marks-card")
    public ResponseEntity<MarksCardDTO> getMarksCard(@PathVariable final Long studentId) {
        MarksCardDTO marksCard = studentService.getMarksCard(studentId);
        return ResponseEntity.ok(marksCard);
    }

    /**
     * Fixes or creates LoginInfo for a student by email.
     * This is useful when a student was created but LoginInfo is missing or incorrect.
     *
     * @param email the student email
     * @param password optional password (if not provided, a random one will be generated)
     * @return success message
     */
    @PostMapping("/fix-login/{email}")
    public ResponseEntity<Map<String, String>> fixStudentLogin(
            @PathVariable final String email,
            @RequestParam(required = false) final String password) {
        try {
            studentService.fixStudentLoginInfo(email, password);
            return ResponseEntity.ok(Map.of("message", "Login credentials fixed successfully for " + email));
        } catch (StudentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Student not found with email: " + email));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error fixing login: " + e.getMessage()));
        }
    }

    /**
     * Syncs passwords from LoginInfo to Student table for all students.
     * Useful for backfilling passwords after adding the password column.
     *
     * @return number of students updated
     */
    @PostMapping("/sync-passwords")
    public ResponseEntity<Map<String, Object>> syncPasswords() {
        try {
            int updated = studentService.syncPasswordsFromLoginInfo();
            return ResponseEntity.ok(Map.of(
                    "message", "Password sync completed successfully",
                    "studentsUpdated", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error syncing passwords: " + e.getMessage()));
        }
    }

    /**
     * Exception handler for StudentNotFoundException.
     *
     * @param ex the exception
     * @return not found response
     */
    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<String> handleStudentNotFound(final StudentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}





