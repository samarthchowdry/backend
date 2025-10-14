package com.studentdetails.details.ServiceImpl;

import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Service.StudentService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    private final List<Student> students = new ArrayList<>();

    @Override
    public List<Student> getAllStudents() {
        return students;
    }

    @Override
    public Student getStudentById(Long id) {
        return students.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Student createStudent(Student student) {
        student.setId((long) (students.size() + 1));
        students.add(student);
        return student;
    }

    @Override
    public Student updateStudent(Long id, Student updatedStudent) {
        for (Student s : students) {
            if (s.getId().equals(id)) {
                s.setName(updatedStudent.getName());
                s.setDob(updatedStudent.getDob());
                return s;
            }
        }
        return null;
    }

    @Override
    public void deleteStudent(Long id) {
        students.removeIf(s -> s.getId().equals(id));
    }
}
