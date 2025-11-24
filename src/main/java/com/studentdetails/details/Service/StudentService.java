package com.studentdetails.details.Service;
import com.studentdetails.details.DTO.MarksCardDTO;
import com.studentdetails.details.DTO.StudentDTO;
import com.studentdetails.details.DTO.StudentMarkDTO;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.DTO.StudentPerformanceDTO;
//
import java.time.LocalDate;
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

    long countStudents();

    List<StudentDTO> getFilteredStudents(String name, LocalDate dateOfBirth, String email, String branch, Long id);

    String bulkUploadStudents(org.springframework.web.multipart.MultipartFile file);
    String bulkUpdateStudents(org.springframework.web.multipart.MultipartFile file);

    StudentMarkDTO addMark(Long studentId, StudentMarkDTO markDTO);

    StudentMarkDTO updateMark(Long studentId, Long markId, StudentMarkDTO markDTO);

    List<StudentMarkDTO> getMarks(Long studentId);

    MarksCardDTO getMarksCard(Long studentId);

    List<StudentPerformanceDTO> getStudentPerformanceSummary();

}

