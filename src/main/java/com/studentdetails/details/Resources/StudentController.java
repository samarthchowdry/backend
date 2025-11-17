package com.studentdetails.details.Resources;

import com.studentdetails.details.DTO.MarksCardDTO;
import com.studentdetails.details.DTO.StudentDTO;
import com.studentdetails.details.DTO.StudentMarkDTO;
import com.studentdetails.details.DTO.StudentPerformanceDTO;
import com.studentdetails.details.Domain.UserRole;
import com.studentdetails.details.Service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

//restcontroller controls it
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }


//    @GetMapping
//      public List<Student> getAllStudents() {
//        return studentService.getAllStudents();
//   }

    @GetMapping
    public ResponseEntity<List<StudentDTO>> getAllStudents(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "dob", required = false) String dob,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "branch", required = false) String branch

    ) {
        LocalDate dateOfBirth = null;
        if (dob != null && !dob.isEmpty()) {
            dateOfBirth = LocalDate.parse(dob);
        }
        return ResponseEntity.ok(studentService.getFilteredStudents(name, dateOfBirth, email,branch));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getStudentCount() {
        return ResponseEntity.ok(studentService.countStudents());
    }


    //    @GetMapping("/{id}")
//    public Student getStudentById(@PathVariable Long id) {
//        return studentService.getStudentById(id);
//    }
    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }


    //    @PostMapping
//    public Student createStudent(@RequestBody Student student) {
//        return studentService.createStudent(student);
//    }
    @PostMapping
    public ResponseEntity<StudentDTO> createStudent(@RequestBody StudentDTO studentDTO,
                                                    @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        UserRole role = resolveRole(roleHeader);
        enforceRole(role, UserRole.ADMIN, UserRole.TEACHER);
        StudentDTO createdStudent = studentService.createStudent(studentDTO);
        return ResponseEntity.ok(createdStudent);
    }


//    @PostMapping("/add")
//    public Student createStudentViaURL(@RequestParam String name, @RequestParam String dob
//    ) {
//        LocalDate dateOfBirth = LocalDate.parse(dob);
//        Student student = new Student(1l, name, dateOfBirth);
//        return studentService.createStudent(student);
//    }

//    @DeleteMapping("/{id}")
//    public String deleteStudent (@PathVariable Long id){
//        studentService.deleteStudent(id);
//        return "Student with ID " + id + " deleted successfully";
//    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> updateStudent(
            @PathVariable Long id,
            @RequestBody StudentDTO studentDTO,
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        UserRole role = resolveRole(roleHeader);
        enforceRole(role, UserRole.ADMIN, UserRole.TEACHER);
        StudentDTO updatedStudent = studentService.updateStudent(id, studentDTO);
        return ResponseEntity.ok(updatedStudent);
    }

//    // UPDATE APIS
//    @PutMapping("/{id}")
//    public Student updateStudent (@PathVariable Long id, @RequestBody Student updatedStudent){
//        return studentService.updateStudent(id, updatedStudent);

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStudent(@PathVariable Long id,
                                                @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        UserRole role = resolveRole(roleHeader);
        enforceRole(role, UserRole.ADMIN);
        studentService.deleteStudent(id);
        return ResponseEntity.ok("Student with ID " + id + " deleted successfully");
    }

    @PostMapping(path = "/bulk-upload", consumes = {"multipart/form-data"})
    public ResponseEntity<String> bulkUpload(@RequestParam("file") MultipartFile file,
                                             @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        UserRole role = resolveRole(roleHeader);
        enforceRole(role, UserRole.ADMIN, UserRole.TEACHER);
        String result = studentService.bulkUploadStudents(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/marks")
    public ResponseEntity<StudentMarkDTO> addMark(@PathVariable Long id,
                                                  @RequestBody StudentMarkDTO markDTO,
                                                  @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        UserRole role = resolveRole(roleHeader);
        enforceRole(role, UserRole.ADMIN, UserRole.TEACHER);
        StudentMarkDTO saved = studentService.addMark(id, markDTO);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/marks/{markId}")
    public ResponseEntity<StudentMarkDTO> updateMark(@PathVariable Long id,
                                                     @PathVariable Long markId,
                                                     @RequestBody StudentMarkDTO markDTO,
                                                     @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        UserRole role = resolveRole(roleHeader);
        enforceRole(role, UserRole.ADMIN, UserRole.TEACHER);
        StudentMarkDTO updated = studentService.updateMark(id, markId, markDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/marks")
    public ResponseEntity<List<StudentMarkDTO>> getMarks(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getMarks(id));
    }

    @GetMapping("/{id}/marks-card")
    public ResponseEntity<MarksCardDTO> getMarksCard(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getMarksCard(id));
    }

    @GetMapping("/performance")
    public ResponseEntity<List<StudentPerformanceDTO>> getPerformanceSummary(
            @RequestHeader(name = "X-Role", required = false) String roleHeader) {
        UserRole role = resolveRole(roleHeader);
        enforceRole(role, UserRole.ADMIN, UserRole.TEACHER);
        return ResponseEntity.ok(studentService.getStudentPerformanceSummary());
    }

    private UserRole resolveRole(String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing role information");
        }
        try {
            return UserRole.valueOf(roleHeader.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid role value");
        }
    }

    private void enforceRole(UserRole role, UserRole... allowedRoles) {
        for (UserRole allowedRole : allowedRoles) {
            if (role == allowedRole) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for role " + role);
    }

}
