package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.DTO.StudentCourseSummaryDTO;
import com.studentdetails.details.DTO.StudentProgressReportDTO;
import com.studentdetails.details.DTO.StudentProgressReportResponseDTO;
import com.studentdetails.details.DTO.StudentSubjectAnalyticsDTO;
import com.studentdetails.details.Domain.Course;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Domain.StudentMark;
import com.studentdetails.details.Repository.StudentRepository;
import com.studentdetails.details.Service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Service implementation for report generation operations.
 * This class is used by Spring Framework for dependency injection.
 */
@Service
@Transactional(readOnly = true)
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class ReportServiceImpl implements ReportService {

    private final StudentRepository studentRepository;

    /**
     * Constructor for ReportServiceImpl.
     * This constructor is used by Spring Framework for dependency injection.
     *
     * @param studentRepository the student repository
     */
    @SuppressWarnings("unused") // Suppress unused warning - constructor is used by Spring Framework
    public ReportServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public StudentProgressReportResponseDTO generateStudentProgressReport() {
        List<Student> students = studentRepository.findAll();
        // Deduplicate students by ID to handle potential duplicates from JOIN FETCH
        Map<Long, Student> uniqueStudents = new LinkedHashMap<>();
        for (Student student : students) {
            if (student != null && student.getId() != null) {
                uniqueStudents.putIfAbsent(student.getId(), student);
            }
        }
        List<StudentProgressReportDTO> reports = uniqueStudents.values().stream()
                .map(this::mapStudent)
                .sorted(Comparator.comparing(dto -> dto.getStudentName() != null
                        ? dto.getStudentName().toLowerCase()
                        : ""))
                .toList();

        long totalAssessments = reports.stream()
                .mapToLong(StudentProgressReportDTO::getTotalAssessments)
                .sum();

        return StudentProgressReportResponseDTO.builder()
                .generatedAt(Instant.now())
                .totalStudents(reports.size())
                .totalAssessments(totalAssessments)
                .students(reports)
                .build();
    }

    private StudentProgressReportDTO mapStudent(Student student) {
        List<StudentCourseSummaryDTO> courses = buildCourseSummary(student.getCourses());
        List<StudentSubjectAnalyticsDTO> subjects = buildSubjectAnalytics(student.getMarks());

        long totalAssessments = subjects.stream()
                .mapToLong(StudentSubjectAnalyticsDTO::getAssessments)
                .sum();
        double totalScore = subjects.stream()
                .mapToDouble(StudentSubjectAnalyticsDTO::getTotalScore)
                .sum();
        double totalMaxScore = subjects.stream()
                .mapToDouble(StudentSubjectAnalyticsDTO::getTotalMaxScore)
                .sum();

        Double overallAverage = totalAssessments > 0 ? totalScore / totalAssessments : null;
        Double overallPercentage = totalMaxScore > 0 ? (totalScore / totalMaxScore) * 100 : null;
        LocalDate lastAssessmentDate = student.getMarks() == null
                ? null
                : student.getMarks().stream()
                .map(StudentMark::getAssessedOn)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        return StudentProgressReportDTO.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .branch(student.getBranch())
                .courses(courses)
                .subjects(subjects)
                .totalAssessments(totalAssessments)
                .overallAverageScore(overallAverage)
                .overallPercentage(overallPercentage)
                .lastAssessmentDate(lastAssessmentDate)
                .build();
    }

    private List<StudentCourseSummaryDTO> buildCourseSummary(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return List.of();
        }

        // Use LinkedHashMap to preserve insertion order and ensure uniqueness by course ID
        Map<Long, StudentCourseSummaryDTO> uniqueCourses = new LinkedHashMap<>();
        for (Course course : courses) {
            if (course != null && course.getId() != null) {
                Long id = course.getId();
                // Only add if not already present (prevents duplicates)
                uniqueCourses.computeIfAbsent(id, courseId -> StudentCourseSummaryDTO.builder()
                        .courseId(courseId)
                        .name(course.getName())
                        .code(course.getCode())
                        .credits(course.getCredits())
                        .build());
            }
        }
        return new ArrayList<>(uniqueCourses.values());
    }

    private List<StudentSubjectAnalyticsDTO> buildSubjectAnalytics(Set<StudentMark> marks) {
        if (marks == null || marks.isEmpty()) {
            return List.of();
        }

        Map<String, SubjectAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (StudentMark mark : marks) {
            if (mark != null) {
                String subjectLabel = normalizeSubject(mark.getSubject());
                SubjectAccumulator accumulator = accumulatorMap.computeIfAbsent(subjectLabel.toLowerCase(),
                        _ -> new SubjectAccumulator(subjectLabel));
                accumulator.accept(mark);
            }
        }

        return accumulatorMap.values()
                .stream()
                .map(SubjectAccumulator::toDto)
                .sorted(Comparator.comparing(StudentSubjectAnalyticsDTO::getPercentage,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private String normalizeSubject(String subject) {
        String trimmed = subject == null ? "" : subject.trim();
        return trimmed.isEmpty() ? "General" : trimmed;
    }

    private static final class SubjectAccumulator {
        private final String subject;
        private long assessments;
        private double totalScore;
        private double totalMaxScore;

        private SubjectAccumulator(String subject) {
            this.subject = subject;
        }

        private void accept(StudentMark mark) {
            double score = mark.getScore() != null ? mark.getScore() : 0.0;
            double maxScore = mark.getMaxScore() != null ? mark.getMaxScore() : 0.0;
            this.totalScore += score;
            this.totalMaxScore += maxScore;
            this.assessments += 1;
        }

        private StudentSubjectAnalyticsDTO toDto() {
            Double average = assessments > 0 ? totalScore / assessments : null;
            Double percentage = totalMaxScore > 0 ? (totalScore / totalMaxScore) * 100 : null;
            return StudentSubjectAnalyticsDTO.builder()
                    .subject(subject)
                    .assessments(assessments)
                    .totalScore(totalScore)
                    .totalMaxScore(totalMaxScore)
                    .averageScore(average)
                    .percentage(percentage)
                    .build();
        }
    }
}

