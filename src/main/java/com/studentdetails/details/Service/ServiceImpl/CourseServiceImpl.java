package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.DTO.CourseDTO;
import com.studentdetails.details.Domain.Course;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Mapper.CourseMapper;
import com.studentdetails.details.Repository.CourseRepository;
import com.studentdetails.details.Repository.StudentRepository;
import com.studentdetails.details.Service.CourseService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CourseMapper courseMapper;

    @Override
    public List<CourseDTO> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        return courseMapper.toDto(courses);
    }

    @Override
    public CourseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        return courseMapper.toDto(course);
    }

    @Override
    public CourseDTO createCourse(CourseDTO courseDTO) {
        Course course = courseMapper.toEntity(courseDTO);
        Course savedCourse = courseRepository.save(course);
        return courseMapper.toDto(savedCourse);
    }

    @Override
    public CourseDTO updateCourse(Long id, CourseDTO courseDTO) {
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        existingCourse.setName(courseDTO.getName());
        existingCourse.setCode(courseDTO.getCode());
        existingCourse.setDescription(courseDTO.getDescription());
        existingCourse.setCredits(courseDTO.getCredits());
        Course updatedCourse = courseRepository.save(existingCourse);
        return courseMapper.toDto(updatedCourse);
    }

    @Override
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException(id);
        }
        courseRepository.deleteById(id);
    }

    @Override
    public List<CourseDTO> getFilteredCourses(String name, String code) {
        List<Course> courses;
        if ((name == null || name.isEmpty()) && (code == null || code.isEmpty())) {
            courses = courseRepository.findAll();
        } else {
            courses = courseRepository.findByFilters(
                    (name != null && !name.isEmpty()) ? name : null,
                    (code != null && !code.isEmpty()) ? code : null
            );
        }
        return courseMapper.toDto(courses);
    }

    @Override
    public List<CourseDTO> getCoursesByStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
        return courseMapper.toDto(student.getCourses());
    }

    @Override
    public CourseDTO addStudentToCourse(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        if (!student.getCourses().contains(course)) {
            student.getCourses().add(course);
            studentRepository.save(student);
        }

        course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        return courseMapper.toDto(course);
    }

    @Override
    public CourseDTO removeStudentFromCourse(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        student.getCourses().remove(course);
        studentRepository.save(student);

        course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        return courseMapper.toDto(course);
    }
}
