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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CourseMapper courseMapper;

    // --- Existing methods unchanged ---
    @Override
    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourses() {
        return courseMapper.toDto(courseRepository.findAll());
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
    @Transactional(readOnly = true)
    public long countCourses() {
        return courseRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDTO> getFilteredCourses(String name, String code) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasCode = code != null && !code.isBlank();

        List<Course> courses = (hasName || hasCode)
                ? courseRepository.findByFilters(hasName ? name : null, hasCode ? code : null)
                : courseRepository.findAll();

        return courseMapper.toDto(courses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDTO> getCoursesByStudent(Long studentId) {
        Student student = studentRepository.findByIdWithCourses(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
        java.util.Map<String, Course> unique = new java.util.LinkedHashMap<>();
        for (Course course : student.getCourses()) {
            if (course == null) {
                continue;
            }
            String key;
            if (course.getId() != null) {
                key = "id:" + course.getId();
            } else {
                String name = course.getName() != null ? course.getName().trim().toLowerCase() : "";
                String code = course.getCode() != null ? course.getCode().trim().toLowerCase() : "";
                key = "name:" + name + "|code:" + code;
            }
            unique.putIfAbsent(key, course);
        }
        return unique.values().stream()
                .map(courseMapper::toDto)
                .toList();
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

    // --- Simplified auth-based methods ---

    public CourseDTO createCourseWithAuth(CourseDTO courseDTO, String roleHeader) {
        enforceAdminRole(roleHeader);
        return createCourse(courseDTO);
    }

    public CourseDTO updateCourseWithAuth(Long id, CourseDTO courseDTO, String roleHeader) {
        enforceAdminRole(roleHeader);
        return updateCourse(id, courseDTO);
    }

    public void deleteCourseWithAuth(Long id, String roleHeader) {
        enforceAdminRole(roleHeader);
        deleteCourse(id);
    }

    public CourseDTO addStudentToCourseWithAuth(Long courseId, Long studentId, String roleHeader) {
        enforceAdminRole(roleHeader);
        return addStudentToCourse(courseId, studentId);
    }

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
        for (Student student : students) {
            boolean alreadyLinked = student.getCourses().stream()
                    .anyMatch(existing -> existing.getId().equals(course.getId()));
            if (!alreadyLinked) {
                student.getCourses().add(course);
                studentRepository.save(student);
            }
        }
    }

    private void detachAll(Course course) {
        List<Student> existingStudents = new ArrayList<>(course.getStudents());
        for (Student student : existingStudents) {
            boolean removed = student.getCourses().removeIf(c -> c.getId().equals(course.getId()));
            if (removed) {
                studentRepository.save(student);
            }
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
        } catch (IllegalArgumentException ex) {
            throw new AuthNotFoundException("Invalid role value: " + roleHeader);
        }
    }

}

