package com.studentdetails.details.Resources;
import com.studentdetails.details.DTO.StudentDTO;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
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
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
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
    public ResponseEntity<StudentDTO> createStudent(@RequestBody StudentDTO studentDTO) {
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
        @RequestBody StudentDTO studentDTO) {
    StudentDTO updatedStudent = studentService.updateStudent(id, studentDTO);
    return ResponseEntity.ok(updatedStudent);
}

//    // UPDATE APIS
//    @PutMapping("/{id}")
//    public Student updateStudent (@PathVariable Long id, @RequestBody Student updatedStudent){
//        return studentService.updateStudent(id, updatedStudent);

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok("Student with ID " + id + " deleted successfully");
    }

}