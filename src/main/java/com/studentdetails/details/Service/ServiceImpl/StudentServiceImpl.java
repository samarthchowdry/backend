package com.studentdetails.details.Service.ServiceImpl;
import com.studentdetails.details.DTO.StudentDTO;
import com.studentdetails.details.Domain.Course;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Mapper.StudentMapper;
import com.studentdetails.details.Repository.CourseRepository;
import com.studentdetails.details.Repository.StudentRepository;
import com.studentdetails.details.Service.StudentService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final StudentMapper studentMapper;  // Inject MapStruct mapper

    @Override
    public List<StudentDTO> getAllStudents() {
        return studentMapper.toDto(studentRepository.findAll());
    }

//    @Override
//    public StudentDTO getStudentById(Long id) {
//        Student student = studentRepository.findById(id).orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
//                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
//                      org.springframework.http.HttpStatus.NOT_FOUND,
//                    "Student not found with id: " + id
//              ));
//        return studentMapper.toDto(student);
    @Override
    public StudentDTO getStudentById(Long id) {
    Student student = studentRepository.findById(id)
            .orElseThrow(() -> new StudentNotFoundException(id));
    return studentMapper.toDto(student);
}

    @Override
    public StudentDTO createStudent(StudentDTO studentDTO) {
        Student student = studentMapper.toEntity(studentDTO);
        // Set courses if courseIds are provided
        if (studentDTO.getCourseIds() != null && !studentDTO.getCourseIds().isEmpty()) {
            List<Course> courses = studentDTO.getCourseIds().stream()
                    .map(id -> courseRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Course not found with id: " + id)))
                    .toList();
            student.setCourses(courses);
        }
        Student savedStudent = studentRepository.save(student);
        return studentMapper.toDto(savedStudent);
    }

//    @Override
//    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
//        Student existingStudent = studentRepository.findById(id).orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
//        existingStudent.setName(studentDTO.getName());
//        existingStudent.setDob(studentDTO.getDob());
//        Student updatedStudent = studentRepository.save(existingStudent);
//        return studentMapper.toDto(updatedStudent);
//    }

    @Override
    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        Student existingStudent = studentRepository.findByIdWithCourses(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
        existingStudent.setName(studentDTO.getName());
        existingStudent.setDob(studentDTO.getDob());
        existingStudent.setEmail(studentDTO.getEmail());
        existingStudent.setAddress(studentDTO.getAddress());
        existingStudent.setBranch(studentDTO.getBranch());
        
        // Update courses if provided
        if (studentDTO.getCourseIds() != null) {
            // Clear existing courses first
            existingStudent.getCourses().clear();
            
            if (!studentDTO.getCourseIds().isEmpty()) {
                // Fetch and add new courses
                studentDTO.getCourseIds().forEach(courseId -> {
                    Course course = courseRepository.findById(courseId)
                            .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
                    existingStudent.getCourses().add(course);
                });
            }
        }
        
        Student updatedStudent = studentRepository.save(existingStudent);
        return studentMapper.toDto(updatedStudent);
    }

//    @Override
//    public void deleteStudent(Long id) {
//        if (!studentRepository.existsById(id)) {
//            throw new RuntimeException("Student not found with id: " + id);
//        }
//        studentRepository.deleteById(id);
    @Override
    public void deleteStudent(Long id) {
    if (!studentRepository.existsById(id)) {
        throw new StudentNotFoundException(id);
    }
    studentRepository.deleteById(id);

    }


    @Override
    public List<StudentDTO> getFilteredStudents(String name, LocalDate dateOfBirth, String email,String branch) {
        List<Student> students;

        // If no filters are provided, fetch all
        if ((name == null || name.isEmpty()) &&
                dateOfBirth == null &&
                (email == null || email.isEmpty())&&
                (branch == null || branch.isEmpty())) {
            students = studentRepository.findAll();
        } else {
            students = studentRepository.findByFilters(
                    (name != null && !name.isEmpty()) ? name : null,
                    dateOfBirth,
                    (email != null && !email.isEmpty()) ? email : null,
                    (branch !=null && !branch.isEmpty()) ?branch: null
            );
        }

        return studentMapper.toDto(students);
    }

}

