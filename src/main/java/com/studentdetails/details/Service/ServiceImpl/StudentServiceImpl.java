package com.studentdetails.details.Service.ServiceImpl;
import com.studentdetails.details.DTO.MarksCardDTO;
import com.studentdetails.details.DTO.StudentDTO;
import com.studentdetails.details.DTO.StudentMarkDTO;
import com.studentdetails.details.DTO.StudentPerformanceDTO;
import com.studentdetails.details.Domain.Course;
import com.studentdetails.details.Domain.GradeScale;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Domain.StudentMark;
import com.studentdetails.details.Exception.StudentNotFoundException;
import com.studentdetails.details.Mapper.StudentMapper;
import com.studentdetails.details.Mapper.StudentMarkMapper;
import com.studentdetails.details.Repository.CourseRepository;
import com.studentdetails.details.Repository.GradeScaleRepository;
import com.studentdetails.details.Repository.StudentMarkRepository;
import com.studentdetails.details.Repository.StudentRepository;
import com.studentdetails.details.Service.EmailService;
import com.studentdetails.details.Service.NotificationService;
import com.studentdetails.details.Service.StudentService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

@AllArgsConstructor
@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final StudentMarkRepository studentMarkRepository;
    private final GradeScaleRepository gradeScaleRepository;
    private final StudentMapper studentMapper;  // Inject MapStruct mapper
    private final StudentMarkMapper studentMarkMapper;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    public List<StudentDTO> getAllStudents() {
        return studentMapper.toDto(studentRepository.findAll());
    }

    @Override
    public StudentDTO getStudentById(Long id) {
        Student student = studentRepository.findByIdWithCourses(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
        return studentMapper.toDto(student);
    }

    @Override
    public StudentDTO createStudent(StudentDTO studentDTO) {
        Student student = studentMapper.toEntity(studentDTO);
        if (student.getId() == null) {
            Long maxId = null;
            try { maxId = studentRepository.findMaxId(); } catch (Exception ignored) {}
            long nextId = (maxId == null ? 0L : maxId) + 1L;
            student.setId(nextId);
        }
        // Set courses if courseIds are provided
        if (studentDTO.getCourseIds() != null && !studentDTO.getCourseIds().isEmpty()) {
            List<Course> courses = fetchDistinctCourses(studentDTO.getCourseIds());
            student.setCourses(new ArrayList<>(courses));
        }
        Student savedStudent = studentRepository.save(student);
        
        // Send email notification
        try {
            String subject = "Welcome to Student Management System";
            String body = String.format("Hello %s,\n\nYou have been successfully added to the Student Management System.\n\nStudent ID: %d\nEmail: %s\nBranch: %s\n\nWelcome aboard!", 
                savedStudent.getName(), savedStudent.getId(), savedStudent.getEmail(), savedStudent.getBranch());
            emailService.sendEmail(savedStudent.getEmail(), subject, body);
        } catch (Exception e) {
            // Log but don't fail the student creation
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
        
        // Create in-app notification for admin
        try {
            notificationService.createNotification(
                "New Student Added",
                String.format("Student %s (ID: %d) has been added to the system.", savedStudent.getName(), savedStudent.getId())
            );
        } catch (Exception e) {
            System.err.println("Failed to create notification: " + e.getMessage());
        }
        
        return studentMapper.toDto(savedStudent);
    }

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
                List<Course> courses = fetchDistinctCourses(studentDTO.getCourseIds());
                existingStudent.getCourses().addAll(courses);
            }
        }
        
        Student updatedStudent = studentRepository.save(existingStudent);
        return studentMapper.toDto(updatedStudent);
    }

    @Override
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new StudentNotFoundException(id);
        }
        studentRepository.deleteById(id);
    }

    @Override
    public long countStudents() {
        return studentRepository.count();
    }

    @Override
    public List<StudentDTO> getFilteredStudents(String name, LocalDate dateOfBirth, String email,String branch, Long id) {
        List<Student> students;

        // If no filters are provided, fetch all
        if ((name == null || name.isEmpty()) &&
                dateOfBirth == null &&
                (email == null || email.isEmpty())&&
                (branch == null || branch.isEmpty()) &&
                id == null) {
            students = studentRepository.findAll();
        } else {
            students = studentRepository.findByFilters(
                    (name != null && !name.isEmpty()) ? name : null,
                    dateOfBirth,
                    (email != null && !email.isEmpty()) ? email : null,
                    (branch !=null && !branch.isEmpty()) ?branch: null,
                    id
            );
        }

        return studentMapper.toDto(students);
    }

    @Override
    public String bulkUploadStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "No file uploaded.";
        }

        int processed = 0;
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

        try (CSVParser parser = CSVParser.parse(
                file.getInputStream(),
                java.nio.charset.StandardCharsets.UTF_8,
                CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .withIgnoreEmptyLines()
                        .withTrim(true)
        )) {
            long nextId = 0L;
            try {
                Long maxId = studentRepository.findMaxId();
                nextId = (maxId == null ? 0L : maxId) + 1L;
            } catch (Exception ignored) {
                // fallback keeps nextId at 1
                nextId = 1L;
            }
            for (CSVRecord record : parser) {
                processed++;
                try {
                    String name = record.get("name");
                    String dobStr = record.get("dob");
                    String email = record.get("email");
                    String address = record.get("address");
                    String branch = record.get("branch");
                    String courseIdsStr = null;
                    if (parser.getHeaderMap().containsKey("courseIds")) {
                        courseIdsStr = record.get("courseIds");
                    }

                    if (name == null || name.isBlank() ||
                            dobStr == null || dobStr.isBlank() ||
                            email == null || email.isBlank() ||
                            address == null || address.isBlank() ||
                            branch == null || branch.isBlank()) {
                        skipped++;
                        errors.add("Row " + processed + ": missing required fields");
                        continue;
                    }

                    LocalDate dob;
                    try {
                        dob = LocalDate.parse(dobStr, formatter);
                    } catch (DateTimeParseException ex) {
                        skipped++;
                        errors.add("Row " + processed + ": invalid dob format (expected yyyy-MM-dd)");
                        continue;
                    }

                    if (studentRepository.existsByEmail(email)) {
                        skipped++;
                        errors.add("Row " + processed + ": email already exists - " + email);
                        continue;
                    }

                    Student student = new Student();
                    // assign id explicitly to avoid relying on native generated values
                    student.setId(nextId++);
                    student.setName(name);
                    student.setDob(dob);
                    student.setEmail(email);
                    student.setAddress(address);
                    student.setBranch(branch);

                    if (courseIdsStr != null && !courseIdsStr.isBlank()) {
                        String[] parts = courseIdsStr.split(",");
                        for (String part : parts) {
                            String trimmed = part.trim();
                            if (trimmed.isEmpty()) continue;
                            Long cid = Long.parseLong(trimmed);
                            Course course = courseRepository.findById(cid)
                                    .orElseThrow(() -> new RuntimeException("Course not found with id: " + cid));
                            student.getCourses().add(course);
                        }
                    }

                    studentRepository.save(student);
                    created++;
                } catch (Exception e) {
                    skipped++;
                    errors.add("Row " + processed + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return "Failed to parse CSV: " + e.getMessage();
        }

        String summary = "Processed: " + processed + ", Created: " + created + ", Skipped: " + skipped;
        if (!errors.isEmpty()) {
            // Return concise errors, not too long
            int limit = Math.min(errors.size(), 10);
            summary += ". Errors (first " + limit + "): " + String.join("; ", errors.subList(0, limit));
        }
        return summary;
    }

    private List<Course> fetchDistinctCourses(List<Long> courseIds) {
        return courseIds.stream()
                .filter(Objects::nonNull)
                .map(Long::longValue)
                .distinct()
                .map(id -> courseRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Course not found with id: " + id)))
                .toList();
    }

    @Override
    public String bulkUpdateStudents(MultipartFile file) {
        return "";
    }

    @Override
    public StudentMarkDTO addMark(Long studentId, StudentMarkDTO markDTO) {
        if (markDTO == null) {
            throw new IllegalArgumentException("Mark details are required");
        }

        Student student = studentRepository.findByIdWithCourses(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        if (markDTO.getSubject() == null || markDTO.getSubject().isBlank()) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (markDTO.getScore() == null) {
            throw new IllegalArgumentException("Score is required");
        }
        if (markDTO.getScore() < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }
        if (markDTO.getMaxScore() != null && markDTO.getMaxScore() <= 0) {
            throw new IllegalArgumentException("Max score must be greater than zero");
        }
        if (markDTO.getMaxScore() != null && markDTO.getScore() > markDTO.getMaxScore()) {
            throw new IllegalArgumentException("Score cannot exceed max score");
        }
        if (markDTO.getAssessedOn() != null && markDTO.getAssessedOn().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Assessment date cannot be in the future");
        }

        StudentMark mark = studentMarkMapper.toEntity(markDTO);
        mark.setId(null);
        mark.setStudent(student);
        mark.setGrade(calculateGrade(markDTO.getScore(), markDTO.getMaxScore()));
        StudentMark saved = studentMarkRepository.save(mark);
        student.getMarks().add(saved);
        
        // Create in-app notification for admin
        try {
            notificationService.createNotification(
                "Marks Updated",
                String.format("Marks for %s in %s have been added/updated. Score: %.2f/%s", 
                    student.getName(), 
                    markDTO.getSubject() != null ? markDTO.getSubject() : "N/A",
                    markDTO.getScore(),
                    markDTO.getMaxScore() != null ? markDTO.getMaxScore().toString() : "N/A")
            );
        } catch (Exception e) {
            System.err.println("Failed to create notification: " + e.getMessage());
        }
        
        return studentMarkMapper.toDto(saved);
    }

    @Override
    public List<StudentMarkDTO> getMarks(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new StudentNotFoundException(studentId);
        }
        List<StudentMark> marks = studentMarkRepository.findByStudentIdOrderByAssessedOnDesc(studentId);
        return studentMarkMapper.toDto(marks);
    }

    @Override
    public StudentMarkDTO updateMark(Long studentId, Long markId, StudentMarkDTO markDTO) {
        if (markDTO == null) {
            throw new IllegalArgumentException("Mark details are required");
        }
        StudentMark mark = studentMarkRepository.findByIdAndStudentId(markId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Mark not found for student"));

        String subject = markDTO.getSubject() != null ? markDTO.getSubject() : mark.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject is required");
        }
        mark.setSubject(subject);

        Double score = markDTO.getScore() != null ? markDTO.getScore() : mark.getScore();
        if (score == null) {
            throw new IllegalArgumentException("Score is required");
        }
        if (score < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }
        mark.setScore(score);

        Double maxScore = markDTO.getMaxScore() != null ? markDTO.getMaxScore() : mark.getMaxScore();
        if (maxScore != null && maxScore <= 0) {
            throw new IllegalArgumentException("Max score must be greater than zero");
        }
        mark.setMaxScore(maxScore);

        if (maxScore != null && score > maxScore) {
            throw new IllegalArgumentException("Score cannot exceed max score");
        }
        if (markDTO.getAssessedOn() != null && markDTO.getAssessedOn().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Assessment date cannot be in the future");
        }

        if (markDTO.getAssessmentName() != null) {
            mark.setAssessmentName(markDTO.getAssessmentName());
        }
        if (markDTO.getGrade() != null) {
            mark.setGrade(markDTO.getGrade());
        }
        if (markDTO.getAssessedOn() != null) {
            mark.setAssessedOn(markDTO.getAssessedOn());
        }
        if (markDTO.getRecordedBy() != null) {
            mark.setRecordedBy(markDTO.getRecordedBy());
        }

        mark.setGrade(calculateGrade(score, maxScore));
        StudentMark saved = studentMarkRepository.save(mark);
        
        // Create in-app notification for admin
        try {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new StudentNotFoundException(studentId));
            notificationService.createNotification(
                "Marks Updated",
                String.format("Marks for %s in %s have been updated. Score: %.2f/%s", 
                    student.getName(), 
                    saved.getSubject() != null ? saved.getSubject() : "N/A",
                    saved.getScore(),
                    saved.getMaxScore() != null ? saved.getMaxScore().toString() : "N/A")
            );
        } catch (Exception e) {
            System.err.println("Failed to create notification: " + e.getMessage());
        }
        
        return studentMarkMapper.toDto(saved);
    }

    @Override
    public MarksCardDTO getMarksCard(Long studentId) {
        Student student = studentRepository.findByIdWithCourses(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
        List<StudentMark> marks = studentMarkRepository.findByStudentIdOrderByAssessedOnDesc(studentId);

        double totalScore = marks.stream()
                .map(StudentMark::getScore)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        double totalMax = marks.stream()
                .map(StudentMark::getMaxScore)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        Double percentage = totalMax > 0 ? (totalScore / totalMax) * 100.0 : null;
        String overallGrade = calculateGrade(
                totalMax > 0 ? totalScore : null,
                totalMax > 0 ? totalMax : null
        );

        List<String> courseNames = extractUniqueCourseNames(student.getCourses());

        return MarksCardDTO.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .branch(student.getBranch())
                .email(student.getEmail())
                .dateOfBirth(student.getDob())
                .courses(courseNames)
                .marks(studentMarkMapper.toDto(marks))
                .totalScore(totalMax > 0 ? totalScore : null)
                .totalMaxScore(totalMax > 0 ? totalMax : null)
                .percentage(percentage)
                .overallGrade(overallGrade)
                .generatedOn(LocalDate.now())
                .build();
    }

    @Override
    public List<StudentPerformanceDTO> getStudentPerformanceSummary() {
        List<StudentPerformanceDTO> summary = new ArrayList<>(studentRepository.fetchPerformanceSummary());
        summary.removeIf(dto -> dto.getTotalAssessments() <= 0);
        summary.sort(Comparator
                .comparingDouble(this::primaryScore)
                .reversed()
                .thenComparing(StudentPerformanceDTO::getStudentName, String.CASE_INSENSITIVE_ORDER));
        return summary;
    }

    private double primaryScore(StudentPerformanceDTO dto) {
        if (dto.getPercentage() != null && Double.isFinite(dto.getPercentage())) {
            return dto.getPercentage();
        }
        if (dto.getAverageScore() != null && Double.isFinite(dto.getAverageScore())) {
            return dto.getAverageScore();
        }
        return 0.0;
    }

    private String calculateGrade(Double score, Double maxScore) {
        if (score == null || maxScore == null || maxScore <= 0) {
            return null;
        }
        double percentage = (score / maxScore) * 100.0;
        List<GradeScale> scales = gradeScaleRepository.findAllByOrderByMinPercentageDesc();
        if (!scales.isEmpty()) {
            for (GradeScale scale : scales) {
                if (scale.getMinPercentage() == null) {
                    continue;
                }
                boolean meetsMin = percentage >= scale.getMinPercentage();
                boolean belowMax = scale.getMaxPercentage() == null || percentage <= scale.getMaxPercentage();
                if (meetsMin && belowMax) {
                    return scale.getGrade();
                }
            }
            // fall through to default chart if no matching rule
        }
        // Fallback default chart if no reference data configured
        if (percentage >= 95) {
            return "Outstanding";
        } else if (percentage >= 90) {
            return "A+";
        } else if (percentage >= 80) {
            return "A";
        } else if (percentage >= 70) {
            return "B+";
        }
        return "B";
    }

    private List<String> extractUniqueCourseNames(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return List.of();
        }

        java.util.Map<String, String> unique = new java.util.LinkedHashMap<>();
        for (Course course : courses) {
            if (course == null) {
                continue;
            }
            String label = course.getName();
            if (label == null || label.isBlank()) {
                continue;
            }

            String key;
            if (course.getId() != null) {
                key = "id:" + course.getId();
            } else {
                String normalized = label.trim().toLowerCase(Locale.ROOT);
                String code = course.getCode() != null ? course.getCode().trim().toLowerCase(Locale.ROOT) : "";
                key = "name:" + normalized + "|code:" + code;
            }

            unique.putIfAbsent(key, label.trim());
        }
        return new java.util.ArrayList<>(unique.values());
    }
}

