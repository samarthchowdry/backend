package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.DTO.CourseDTO;
import com.studentdetails.details.Domain.Course;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Domain.UserRole;
import com.studentdetails.details.Exception.AuthNotFoundException;
import com.studentdetails.details.Exception.CourseNotFoundException;
import com.studentdetails.details.Exception.StudentNotFoundException;
import com.studentdetails.details.Mapper.CourseMapper;
import com.studentdetails.details.Repository.CourseRepository;
import com.studentdetails.details.Repository.StudentRepository;
import com.studentdetails.details.Service.CourseService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation for course-related operations.
 * This class is used by Spring Framework for dependency injection.
 */
@AllArgsConstructor
@Service
@Transactional
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class CourseServiceImpl implements CourseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CourseMapper courseMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        return courses.stream()
                .map(courseMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        return courseMapper.toDto(course);
    }

    @Override
    public CourseDTO createCourse(CourseDTO courseDTO) {
        if (courseDTO == null) {
            throw new IllegalArgumentException("Course details are required");
        }

        Course course = courseMapper.toEntity(courseDTO);
        course.setId(null);

        List<Student> students = loadStudents(courseDTO.getStudentIds());
        course.setStudents(new ArrayList<>(students));

        Course saved = courseRepository.save(course);
        syncStudents(saved, students);

        return courseMapper.toDto(saved);
    }

    @Override
    public CourseDTO updateCourse(Long id, CourseDTO courseDTO) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));

        if (courseDTO.getName() != null) {
            course.setName(courseDTO.getName());
        }
        if (courseDTO.getCode() != null) {
            course.setCode(courseDTO.getCode());
        }
        if (courseDTO.getDescription() != null) {
            course.setDescription(courseDTO.getDescription());
        }
        if (courseDTO.getCredits() != null) {
            course.setCredits(courseDTO.getCredits());
        }

        if (courseDTO.getStudentIds() != null) {
            List<Student> students = loadStudents(courseDTO.getStudentIds());
            detachAll(course);
            course.getStudents().clear();
            course.getStudents().addAll(students);
            syncStudents(course, students);
        }

        Course saved = courseRepository.save(course);
        return courseMapper.toDto(saved);
    }

    @Override
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        detachAll(course);
        courseRepository.delete(course);
    }

    @Override
    public CourseDTO addStudentToCourse(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        Student student = studentRepository.findByIdWithCourses(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        if (course.getStudents().stream().noneMatch(s -> s.getId().equals(studentId))) {
            course.getStudents().add(student);
        }
        if (student.getCourses().stream().noneMatch(c -> c.getId().equals(courseId))) {
            student.getCourses().add(course);
        }

        studentRepository.save(student);
        Course saved = courseRepository.save(course);
        return courseMapper.toDto(saved);
    }

    @Override
    public CourseDTO removeStudentFromCourse(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        Student student = studentRepository.findByIdWithCourses(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        course.getStudents().removeIf(s -> s.getId().equals(studentId));
        student.getCourses().removeIf(c -> c.getId().equals(courseId));

        studentRepository.save(student);
        Course saved = courseRepository.save(course);
        return courseMapper.toDto(saved);
    }

    /**
     * Creates a course with admin authentication check.
     *
     * @param courseDTO the course data
     * @param roleHeader the role header for authentication
     * @return the created course DTO
     */
    public CourseDTO createCourseWithAuth(CourseDTO courseDTO, String roleHeader) {
        enforceAdminRole(roleHeader);
        return createCourse(courseDTO);
    }

    /**
     * Updates a course with admin authentication check.
     *
     * @param id the course ID
     * @param courseDTO the course data
     * @param roleHeader the role header for authentication
     * @return the updated course DTO
     */
    public CourseDTO updateCourseWithAuth(Long id, CourseDTO courseDTO, String roleHeader) {
        enforceAdminRole(roleHeader);
        return updateCourse(id, courseDTO);
    }

    /**
     * Deletes a course with admin authentication check.
     *
     * @param id the course ID
     * @param roleHeader the role header for authentication
     */
    public void deleteCourseWithAuth(Long id, String roleHeader) {
        enforceAdminRole(roleHeader);
        deleteCourse(id);
    }

    /**
     * Adds a student to a course with admin authentication check.
     *
     * @param courseId the course ID
     * @param studentId the student ID
     * @param roleHeader the role header for authentication
     * @return the updated course DTO
     */
    public CourseDTO addStudentToCourseWithAuth(Long courseId, Long studentId, String roleHeader) {
        enforceAdminRole(roleHeader);
        return addStudentToCourse(courseId, studentId);
    }

    /**
     * Removes a student from a course with admin authentication check.
     *
     * @param courseId the course ID
     * @param studentId the student ID
     * @param roleHeader the role header for authentication
     * @return the updated course DTO
     */
    public CourseDTO removeStudentFromCourseWithAuth(Long courseId, Long studentId, String roleHeader) {
        enforceAdminRole(roleHeader);
        return removeStudentFromCourse(courseId, studentId);
    }

    private List<Student> loadStudents(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }

        List<Student> students = studentRepository.findAllById(studentIds);
        Set<Long> foundIds = students.stream()
                .map(Student::getId)
                .collect(Collectors.toSet());

        studentIds.stream()
                .filter(id -> !foundIds.contains(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new StudentNotFoundException(id);
                });

        return students;
    }

    private void syncStudents(Course course, List<Student> students) {
        if (students == null || students.isEmpty()) {
            return;
        }
        // Batch update: collect all students that need updating, then save once
        List<Student> toUpdate = new ArrayList<>();
        for (Student student : students) {
            boolean alreadyLinked = student.getCourses().stream()
                    .anyMatch(existing -> existing.getId().equals(course.getId()));
            if (!alreadyLinked) {
                student.getCourses().add(course);
                toUpdate.add(student);
            }
        }
        // Batch save all students at once
        if (!toUpdate.isEmpty()) {
            studentRepository.saveAll(toUpdate);
        }
    }

    private void detachAll(Course course) {
        List<Student> existingStudents = new ArrayList<>(course.getStudents());
        if (existingStudents.isEmpty()) {
            course.getStudents().clear();
            return;
        }
        // Batch update: collect all students that need updating, then save once
        List<Student> toUpdate = new ArrayList<>();
        for (Student student : existingStudents) {
            boolean removed = student.getCourses().removeIf(c -> c.getId().equals(course.getId()));
            if (removed) {
                toUpdate.add(student);
            }
        }
        // Batch save all students at once
        if (!toUpdate.isEmpty()) {
            studentRepository.saveAll(toUpdate);
        }
        course.getStudents().clear();
    }

    // --- Auth helper method ---
    private void enforceAdminRole(String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            throw new AuthNotFoundException("Missing role header");
        }

        try {
            UserRole role = UserRole.valueOf(roleHeader.trim().toUpperCase());
            if (role != UserRole.ADMIN) {
                throw new AuthNotFoundException("Access denied for role: " + role);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Invalid role value provided: {}", roleHeader, e);
            throw new AuthNotFoundException("Invalid role value: " + roleHeader);
        }
    }

}

