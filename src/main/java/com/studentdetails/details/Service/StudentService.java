package com.studentdetails.details.Service;
import com.studentdetails.details.DTO.StudentDTO;
import com.studentdetails.details.Domain.Student;
import java.util.List;
public interface StudentService {
//    List<Student> getAllStudents();
//    Student getStudentById(Long id);
//    Student createStudent(Student student);
//    Student updateStudent(Long id, Student student);
//    void deleteStudent(Long id);

    List<StudentDTO> getAllStudents();
    StudentDTO getStudentById(Long id);
    StudentDTO createStudent(StudentDTO studentDTO);
    StudentDTO updateStudent(Long id, StudentDTO studentDTO);
    void deleteStudent(Long id);
}

