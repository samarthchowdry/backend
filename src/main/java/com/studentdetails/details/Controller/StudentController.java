package com.studentdetails.details.Controller;

import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Service.StudentService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }


    @GetMapping
    public List<Student> getAllStudents() {
        return studentService.getAllStudents();
    }


    @GetMapping("/{id}")
    public Student getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id);
    }


    @PostMapping
    public Student createStudent(@RequestBody Student student) {
        return studentService.createStudent(student);
    }


    @PostMapping("/add")
    public Student createStudentViaURL(@RequestParam String name, @RequestParam String dob
    ) {
        LocalDate dateOfBirth = LocalDate.parse(dob);
        Student student = new Student(name, dateOfBirth);
        return studentService.createStudent(student);
    }

    @DeleteMapping("/{id}")
    public String deleteStudent (@PathVariable Long id){
        studentService.deleteStudent(id);
        return "Student with ID " + id + " deleted successfully";
    }

    // UPDATE APIS
    @PutMapping("/{id}")
    public Student updateStudent (@PathVariable Long id, @RequestBody Student updatedStudent){
        return studentService.updateStudent(id, updatedStudent);
    }

}