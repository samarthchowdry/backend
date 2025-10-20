package com.studentdetails.details.Service.ServiceImpl;
import com.studentdetails.details.DTO.StudentDTO;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Mapper.StudentMapper;
import com.studentdetails.details.Service.StudentService;
import com.studentdetails.details.repository.StudentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository; // single repository field

//    @Override
//    public List<Student> getAllStudents() {
//        return studentRepository.findAll();
//    }

    @Override
    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(StudentMapper::toDTO)
                .collect(Collectors.toList());
    }

//    @Override
//    public Student getStudentById(Long id) {
//        Optional<Student> student = studentRepository.findById(id);
//        return student.orElse(null);
//    }

    @Override
    public StudentDTO getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
        return StudentMapper.toDTO(student);
    }

//    @Override
//    public StudentDTO createStudent(StudentDTO studentDTO) {
//        return null;
//    }

    @Override
    public StudentDTO createStudent(StudentDTO studentDTO) {
        Student student = StudentMapper.toEntity(studentDTO);
        Student savedStudent = studentRepository.save(student);
        return StudentMapper.toDTO(savedStudent);
    }

//    @Override
//    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
//        return null;
//    }

    @Override
    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));

        existingStudent.setName(studentDTO.getName());
        existingStudent.setDob(java.time.LocalDate.parse(studentDTO.getDob()));

        Student updatedStudent = studentRepository.save(existingStudent);
        return StudentMapper.toDTO(updatedStudent);
    }

//    @Override
//    public Student createStudent(Student student) {
//        return studentRepository.save(student); // saves to DB
//    }

//    @Override
//    public Student updateStudent(Long id, Student updatedStudent) {
//        if (studentRepository.existsById(id)) {
//            updatedStudent.setId(id);
//            return studentRepository.save(updatedStudent);
//        }
//        return null;
//    }

    //    @Override
//    public void deleteStudent(Long id) {
//        studentRepository.deleteById(id);
    @Override
    public void deleteStudent(Long id){
        if (!studentRepository.existsById(id)) {
            throw new StudentNotFoundException(id);
        }
        studentRepository.deleteById(id);
    }
}
