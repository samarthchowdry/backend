package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.DTO.MarksCardDTO;
import com.studentdetails.details.DTO.StudentDTO;
import com.studentdetails.details.DTO.StudentMarkDTO;
import com.studentdetails.details.DTO.StudentPerformanceDTO;
import com.studentdetails.details.Domain.Course;
import com.studentdetails.details.Domain.GradeScale;
import com.studentdetails.details.Domain.LoginInfo;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.Domain.StudentMark;
import com.studentdetails.details.Domain.UserRole;
import com.studentdetails.details.Exception.StudentNotFoundException;
import com.studentdetails.details.Mapper.StudentMapper;
import com.studentdetails.details.Mapper.StudentMarkMapper;
import com.studentdetails.details.Repository.CourseRepository;
import com.studentdetails.details.Repository.GradeScaleRepository;
import com.studentdetails.details.Repository.LoginInfoRepository;
import com.studentdetails.details.Repository.StudentMarkRepository;
import com.studentdetails.details.Repository.StudentRepository;
import com.studentdetails.details.Service.EmailService;
import com.studentdetails.details.Service.NotificationService;
import com.studentdetails.details.Service.StudentService;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service implementation for student-related operations.
 * This class is used by Spring Framework for dependency injection.
 */
@AllArgsConstructor
@Service
@org.springframework.transaction.annotation.Transactional
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class StudentServiceImpl implements StudentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentServiceImpl.class);
    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final int MAX_ERROR_DISPLAY_COUNT = 10;
    private static final double GRADE_OUTSTANDING_THRESHOLD = 95.0;
    private static final double GRADE_A_PLUS_THRESHOLD = 90.0;
    private static final double GRADE_A_THRESHOLD = 80.0;
    private static final double GRADE_B_PLUS_THRESHOLD = 70.0;

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final StudentMarkRepository studentMarkRepository;
    private final GradeScaleRepository gradeScaleRepository;
    private final StudentMapper studentMapper;
    private final StudentMarkMapper studentMarkMapper;
    private final LoginInfoRepository loginInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<StudentDTO> getAllStudents() {
        return studentMapper.toDto(studentRepository.findAll());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public StudentDTO getStudentById(Long id) {
        Student student = studentRepository.findByIdWithCourses(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
        return studentMapper.toDto(student);
    }

    @Override
    public StudentDTO createStudent(StudentDTO studentDTO) {
        // Validate email
        if (studentDTO.getEmail() == null || studentDTO.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        String email = studentDTO.getEmail().trim().toLowerCase();
        String password = studentDTO.getPassword();
        String name = studentDTO.getName();
        
        // Step 1: Create LoginInfo FIRST in login_info table (similar to how teachers are created)
        // This ensures the student can login immediately with email and password
        // CRITICAL: This MUST succeed before creating the Student record
        LoginInfo loginInfo;
        try {
            loginInfo = createOrUpdateLoginInfoForStudent(email, name, password);
            LOGGER.info("LoginInfo created/updated in login_info table for student: {} ({}) with ID: {}", 
                    name, email, loginInfo.getId());
            
            // Verify LoginInfo was actually saved
            if (loginInfo.getId() == null) {
                throw new RuntimeException("LoginInfo was not saved - ID is null");
            }
            
            // Verify password was set
            if (loginInfo.getPassword() == null || loginInfo.getPassword().isBlank()) {
                throw new RuntimeException("LoginInfo password was not set");
            }
            
            LOGGER.info("Verified LoginInfo saved successfully: ID={}, Email={}, Role={}, HasPassword={}", 
                    loginInfo.getId(), loginInfo.getEmail(), loginInfo.getRole(), 
                    loginInfo.getPassword() != null && !loginInfo.getPassword().isBlank());
        } catch (Exception e) {
            LOGGER.error("CRITICAL ERROR: Failed to create LoginInfo in login_info table for student {} ({}): {}", 
                    name, email, e.getMessage(), e);
            throw new RuntimeException("Failed to create login credentials in login_info table. Student cannot be created without login access. Error: " + e.getMessage(), e);
        }
        
        // Step 2: Create Student record in students table
        Student student = studentMapper.toEntity(studentDTO);
        if (student.getId() == null) {
            Long maxId = null;
            try {
                maxId = studentRepository.findMaxId();
            } catch (Exception _) {
                // Ignore exception and use default ID generation
                LOGGER.debug("Could not fetch max ID, will use default ID generation");
            }
            long nextId = (maxId == null ? 0L : maxId) + 1L;
            student.setId(nextId);
        }
        
        // Ensure email matches LoginInfo (case-insensitive)
        student.setEmail(email);
        
        // Hash and store password in Student table (in addition to LoginInfo)
        if (password != null && !password.isBlank()) {
            String hashedPassword = passwordEncoder.encode(password.trim());
            student.setPassword(hashedPassword);
            LOGGER.info("Password hashed and stored in Student table for: {} ({})", name, email);
        } else {
            // If no password provided, use the same password from LoginInfo (already hashed)
            // This ensures consistency between Student and LoginInfo tables
            if (loginInfo.getPassword() != null) {
                student.setPassword(loginInfo.getPassword());
                LOGGER.info("Password copied from LoginInfo to Student table for: {} ({})", name, email);
            }
        }
        
        // Set courses if courseIds are provided
        if (studentDTO.getCourseIds() != null && !studentDTO.getCourseIds().isEmpty()) {
            List<Course> courses = fetchDistinctCourses(studentDTO.getCourseIds());
            student.setCourses(new ArrayList<>(courses));
        }
        
        Student savedStudent = studentRepository.save(student);
        
        LOGGER.info("Student created successfully in students table: {} ({}) with Student ID: {} and LoginInfo ID: {}", 
                savedStudent.getName(), savedStudent.getEmail(), savedStudent.getId(), loginInfo.getId());

        // Send email notification
        try {
            String subject = "Welcome to Student Management System";
            String body = String.format("Hello %s,%n%nYou have been successfully added to the Student Management System.%n%nStudent ID: %d%nEmail: %s%nBranch: %s%n%nWelcome aboard!",
                    savedStudent.getName(), savedStudent.getId(), savedStudent.getEmail(), savedStudent.getBranch());
            emailService.sendEmail(savedStudent.getEmail(), subject, body);
        } catch (Exception _) {
            // Log but don't fail the student creation
            LOGGER.warn("Failed to send welcome email for student ID: {}", savedStudent.getId());
        }

        // Create in-app notification for admin
        try {
            notificationService.createNotification(
                    "New Student Added",
                    String.format("Student %s (ID: %d) has been added to the system.", savedStudent.getName(), savedStudent.getId())
            );
        } catch (Exception _) {
            LOGGER.warn("Failed to create notification for student ID: {}", savedStudent.getId());
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

        // Update password in Student table if provided
        if (studentDTO.getPassword() != null && !studentDTO.getPassword().isBlank()) {
            String hashedPassword = passwordEncoder.encode(studentDTO.getPassword().trim());
            existingStudent.setPassword(hashedPassword);
            LOGGER.info("Password updated in Student table for student ID: {}", id);
        }

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
        // Update or create login info; if password supplied, update it, otherwise keep existing
        // Use the same pattern as createStudent - create LoginInfo first
        String email = updatedStudent.getEmail() != null ? updatedStudent.getEmail().trim().toLowerCase() : null;
        if (email != null && !email.isBlank()) {
            try {
                createOrUpdateLoginInfoForStudent(email, updatedStudent.getName(), studentDTO.getPassword());
                LOGGER.info("Successfully updated LoginInfo for student: {} ({})", updatedStudent.getName(), email);
            } catch (Exception e) {
                LOGGER.error("Failed to update LoginInfo for student {} ({}): {}", 
                        updatedStudent.getName(), email, e.getMessage(), e);
                throw new RuntimeException("Student updated but failed to update login credentials. Error: " + e.getMessage(), e);
            }
        }
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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public long countStudents() {
        return studentRepository.count();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<StudentDTO> getFilteredStudents(String name, LocalDate dateOfBirth, String email, String branch, Long id) {
        List<Student> students;

        // If no filters are provided, fetch all
        if ((name == null || name.isEmpty()) &&
                dateOfBirth == null &&
                (email == null || email.isEmpty()) &&
                (branch == null || branch.isEmpty()) &&
                id == null) {
            students = studentRepository.findAll();
        } else {
            students = studentRepository.findByFilters(
                    (name != null && !name.isEmpty()) ? name : null,
                    dateOfBirth,
                    (email != null && !email.isEmpty()) ? email : null,
                    (branch != null && !branch.isEmpty()) ? branch : null,
                    id
            );
        }

        return studentMapper.toDto(students);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
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
                CSVFormat.DEFAULT.builder()
                        .setHeader((String[]) null)
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .build()
        )) {
            long nextId = calculateNextId();

            // Pre-fetch all courses to avoid N+1 queries
            Map<Long, Course> courseCache = courseRepository.findAll().stream()
                    .collect(java.util.stream.Collectors.toMap(Course::getId, course -> course, (a, _) -> a));

            // Collect all records first (CSV parser can only be iterated once)
            List<CSVRecord> allRecords = new ArrayList<>();
            Set<String> csvEmails = new HashSet<>();
            Map<String, Integer> headerMap = parser.getHeaderMap();

            for (CSVRecord record : parser) {
                allRecords.add(record);
                try {
                    String email = record.get("email");
                    if (email != null && !email.isBlank()) {
                        csvEmails.add(email.toLowerCase().trim());
                    }
                } catch (IllegalArgumentException _) {
                    // Skip if email column doesn't exist in CSV
                    LOGGER.debug("Email column not found in CSV record, skipping");
                }
            }

            // Batch check existing emails (single optimized query - only fetches emails, not full entities)
            Set<String> existingEmails = csvEmails.isEmpty()
                    ? new HashSet<>()
                    : new HashSet<>(studentRepository.findAllEmailAddresses());

            // Process all records
            for (CSVRecord record : allRecords) {
                processed++;
                ProcessResult result = processStudentRecord(record, headerMap, formatter, existingEmails, courseCache, nextId);
                if (result.success()) {
                    created++;
                    nextId = result.nextId();
                } else {
                    skipped++;
                    errors.add("Row " + processed + ": " + result.errorMessage());
                }
            }
        } catch (Exception ex) {
            return "Failed to parse CSV: " + ex.getMessage();
        }

        String summary = "Processed: " + processed + ", Created: " + created + ", Skipped: " + skipped;
        if (!errors.isEmpty()) {
            int limit = Math.min(errors.size(), MAX_ERROR_DISPLAY_COUNT);
            summary += ". Errors (first " + limit + "): " + String.join("; ", errors.subList(0, limit));
        }
        return summary;
    }

    /**
     * Creates or updates LoginInfo for a student (similar to how teachers are created).
     * This method is called FIRST before creating the Student record to ensure login works.
     * 
     * @param email the student email (will be normalized to lowercase)
     * @param name the student name
     * @param rawPassword the plain text password (if null/blank, a random one will be generated)
     * @return the created/updated LoginInfo
     */
    private LoginInfo createOrUpdateLoginInfoForStudent(String email, String name, String rawPassword) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        String emailLower = email.trim().toLowerCase();
        boolean hasProvidedPassword = rawPassword != null && !rawPassword.isBlank();

        String passwordToUse;
        if (hasProvidedPassword) {
            String trimmed = rawPassword.trim();
            if (trimmed.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters long");
            }
            passwordToUse = trimmed;
        } else {
            passwordToUse = generateRandomPassword();
            LOGGER.debug("Generated random password for student: {}", emailLower);
        }

        // Check if LoginInfo already exists
        Optional<LoginInfo> existingLoginInfo = loginInfoRepository.findByEmailIgnoreCase(emailLower);
        
        LoginInfo loginInfo;
        boolean isNew = existingLoginInfo.isEmpty();
        
        if (isNew) {
            // Create new LoginInfo (similar to teacher creation)
            String googleSub = "manual-" + UUID.randomUUID().toString();
            String hashedPassword = passwordEncoder.encode(passwordToUse);
            
            loginInfo = LoginInfo.builder()
                    .googleSub(googleSub)
                    .email(emailLower)
                    .fullName(name != null && !name.isBlank() ? name.trim() : null)
                    .role(UserRole.STUDENT)
                    .password(hashedPassword)
                    .lastLoginAt(LocalDateTime.now())
                    .projectAdmin(false)
                    .build();
            
            LOGGER.info("Creating new LoginInfo for student: {} ({})", name, emailLower);
        } else {
            // Update existing LoginInfo
            loginInfo = existingLoginInfo.get();
            LOGGER.info("Updating existing LoginInfo for student: {} ({})", name, emailLower);
            
            if (loginInfo.getGoogleSub() == null || loginInfo.getGoogleSub().isBlank()) {
                loginInfo.setGoogleSub("manual-" + UUID.randomUUID().toString());
            }
            loginInfo.setEmail(emailLower);
            loginInfo.setFullName(name != null && !name.isBlank() ? name.trim() : loginInfo.getFullName());
            loginInfo.setRole(UserRole.STUDENT);
            if (loginInfo.getLastLoginAt() == null) {
                loginInfo.setLastLoginAt(LocalDateTime.now());
            }
            
            // Always update password if provided, or if no password exists
            if (hasProvidedPassword || loginInfo.getPassword() == null || loginInfo.getPassword().isBlank()) {
                loginInfo.setPassword(passwordEncoder.encode(passwordToUse));
                LOGGER.info("Password updated for student: {} ({})", name, emailLower);
            }
        }

        try {
            // Save LoginInfo to database - this MUST succeed
            LoginInfo saved = loginInfoRepository.save(loginInfo);
            
            // CRITICAL: Verify the save was successful
            if (saved == null) {
                LOGGER.error("CRITICAL: loginInfoRepository.save() returned null for student: {} ({})", name, emailLower);
                throw new RuntimeException("Failed to save LoginInfo - repository returned null");
            }
            
            if (saved.getId() == null) {
                LOGGER.error("CRITICAL: LoginInfo saved but ID is null for student: {} ({})", name, emailLower);
                throw new RuntimeException("Failed to save LoginInfo - ID was not generated");
            }
            
            // Verify password was saved
            if (saved.getPassword() == null || saved.getPassword().isBlank()) {
                LOGGER.error("CRITICAL: LoginInfo saved but password is null/blank for student: {} ({})", name, emailLower);
                throw new RuntimeException("Failed to save LoginInfo - password was not set");
            }
            
            // Verify email was saved
            if (saved.getEmail() == null || saved.getEmail().isBlank()) {
                LOGGER.error("CRITICAL: LoginInfo saved but email is null/blank for student: {} ({})", name, emailLower);
                throw new RuntimeException("Failed to save LoginInfo - email was not set");
            }
            
            // Double-check by querying the database to confirm it was actually saved
            Optional<LoginInfo> verifySaved = loginInfoRepository.findById(saved.getId());
            if (verifySaved.isEmpty()) {
                LOGGER.error("CRITICAL: LoginInfo with ID {} was not found in database after save for student: {} ({})", 
                        saved.getId(), name, emailLower);
                throw new RuntimeException("Failed to save LoginInfo - record not found in database after save");
            }
            
            LoginInfo verified = verifySaved.get();
            LOGGER.info("LoginInfo {} successfully and VERIFIED in database for student: {} ({}) with ID: {}, Email: {}, Role: {}, PasswordSet: {}", 
                    isNew ? "created" : "updated", name, emailLower, verified.getId(), verified.getEmail(), verified.getRole(), 
                    verified.getPassword() != null && !verified.getPassword().isBlank());
            
            return verified;
        } catch (Exception e) {
            LOGGER.error("CRITICAL ERROR: Failed to save LoginInfo to database for student: {} ({}): {}", 
                    name, emailLower, e.getMessage(), e);
            throw new RuntimeException("Failed to create/update login credentials in database: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy method - kept for backward compatibility.
     * Now delegates to createOrUpdateLoginInfoForStudent.
     */
    private void ensureLoginInfoForStudent(Student student, String rawPassword) {
        if (student == null || student.getEmail() == null || student.getEmail().isBlank()) {
            LOGGER.warn("Cannot create LoginInfo: student or email is null/blank");
            return;
        }

        String email = student.getEmail().trim().toLowerCase();
        String name = student.getName();
        boolean hasProvidedPassword = rawPassword != null && !rawPassword.isBlank();

        String passwordToUse;
        if (hasProvidedPassword) {
            String trimmed = rawPassword.trim();
            if (trimmed.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters long");
            }
            passwordToUse = trimmed;
        } else {
            passwordToUse = generateRandomPassword();
            LOGGER.debug("Generated random password for student: {}", email);
        }

        // Use case-insensitive email lookup
        LoginInfo loginInfo = loginInfoRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean isNew = loginInfo == null;
        
        // Encode password once
        String encodedPassword = passwordEncoder.encode(passwordToUse);
        LOGGER.debug("Password encoded for student: {} (hasProvided: {})", email, hasProvidedPassword);
        
        if (loginInfo == null) {
            loginInfo = LoginInfo.builder()
                    .googleSub("manual-" + UUID.randomUUID())
                    .email(email)
                    .fullName(name)
                    .role(UserRole.STUDENT)
                    .password(encodedPassword)
                    .lastLoginAt(LocalDateTime.now())
                    .projectAdmin(false)
                    .build();
            LOGGER.info("Creating new LoginInfo for student: {} ({}) with password set", name, email);
        } else {
            LOGGER.info("Updating existing LoginInfo for student: {} ({})", name, email);
            if (loginInfo.getGoogleSub() == null || loginInfo.getGoogleSub().isBlank()) {
                loginInfo.setGoogleSub("manual-" + UUID.randomUUID());
            }
            loginInfo.setEmail(email);
            loginInfo.setFullName(name != null ? name : loginInfo.getFullName());
            loginInfo.setRole(UserRole.STUDENT);
            if (loginInfo.getLastLoginAt() == null) {
                loginInfo.setLastLoginAt(LocalDateTime.now());
            }
            // Always update password if provided, or if no password exists
            boolean shouldUpdatePassword = hasProvidedPassword || loginInfo.getPassword() == null || loginInfo.getPassword().isBlank();
            if (shouldUpdatePassword) {
                loginInfo.setPassword(encodedPassword);
                LOGGER.info("Password updated for student: {} ({}) - hasProvided: {}, existingPasswordNull: {}", 
                        name, email, hasProvidedPassword, loginInfo.getPassword() == null);
            } else {
                LOGGER.debug("Password not updated for student: {} ({}) - hasProvided: {}, existingPassword: {}", 
                        name, email, hasProvidedPassword, loginInfo.getPassword() != null ? "exists" : "null");
            }
        }

        try {
            loginInfoRepository.save(loginInfo);
            LOGGER.info("LoginInfo {} successfully for student: {} ({})", isNew ? "created" : "updated", name, email);
        } catch (Exception e) {
            LOGGER.error("Failed to save LoginInfo for student: {} ({}): {}", name, email, e.getMessage(), e);
            throw new RuntimeException("Failed to create/update login credentials: " + e.getMessage(), e);
        }
    }

    private String generateRandomPassword() {
        // 12-character random string using UUID without hyphens
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * Calculates the next available student ID.
     *
     * @return the next ID to use
     */
    private long calculateNextId() {
        try {
            Long maxId = studentRepository.findMaxId();
            return (maxId == null ? 0L : maxId) + 1L;
        } catch (Exception _) {
            // Fallback keeps nextId at 1 if max ID query fails
            LOGGER.debug("Could not fetch max ID for bulk upload, using fallback ID");
            return 1L;
        }
    }

    /**
     * Result record for processing a student record.
     *
     * @param success whether the processing was successful
     * @param nextId the next ID to use
     * @param errorMessage the error message if processing failed
     */
    private record ProcessResult(boolean success, long nextId, String errorMessage) {
    }

    /**
     * Processes a single student record from CSV.
     *
     * @param record the CSV record
     * @param headerMap the header map
     * @param formatter the date formatter
     * @param existingEmails set of existing emails
     * @param courseCache the course cache
     * @param nextId the next ID to use
     * @return the process result
     */
    private ProcessResult processStudentRecord(CSVRecord record, Map<String, Integer> headerMap,
                                                DateTimeFormatter formatter, Set<String> existingEmails,
                                                Map<Long, Course> courseCache, long nextId) {
        try {
            String name = record.get("name");
            String dobStr = record.get("dob");
            String email = record.get("email");
            String address = record.get("address");
            String branch = record.get("branch");
            String courseIdsStr = headerMap.containsKey("courseIds") ? record.get("courseIds") : null;

            if (name == null || name.isBlank() ||
                    dobStr == null || dobStr.isBlank() ||
                    email == null || email.isBlank() ||
                    address == null || address.isBlank() ||
                    branch == null || branch.isBlank()) {
                return new ProcessResult(false, nextId, "missing required fields");
            }

            LocalDate dob;
            try {
                dob = LocalDate.parse(dobStr, formatter);
            } catch (DateTimeParseException _) {
                return new ProcessResult(false, nextId, "invalid dob format (expected yyyy-MM-dd)");
            }

            String emailLower = email.toLowerCase().trim();
            if (existingEmails.contains(emailLower)) {
                return new ProcessResult(false, nextId, "email already exists - " + email);
            }
            existingEmails.add(emailLower); // Track within CSV too

            Student student = new Student();
            student.setId(nextId);
            student.setName(name);
            student.setDob(dob);
            student.setEmail(email);
            student.setAddress(address);
            student.setBranch(branch);

            if (courseIdsStr != null && !courseIdsStr.isBlank()) {
                processCourseIds(courseIdsStr, courseCache, student);
            }

            studentRepository.save(student);
            return new ProcessResult(true, nextId + 1, null);
        } catch (Exception ex) {
            return new ProcessResult(false, nextId, ex.getMessage());
        }
    }

    /**
     * Processes course IDs from a comma-separated string.
     *
     * @param courseIdsStr the course IDs string
     * @param courseCache the course cache
     * @param student the student to add courses to
     * @throws IllegalArgumentException if course ID is invalid or not found
     */
    private void processCourseIds(String courseIdsStr, Map<Long, Course> courseCache, Student student) {
        String[] parts = courseIdsStr.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    Long cid = Long.parseLong(trimmed);
                    Course course = courseCache.get(cid);
                    if (course == null) {
                        throw new IllegalArgumentException("Course not found with id: " + cid);
                    }
                    student.getCourses().add(course);
                } catch (NumberFormatException _) {
                    throw new IllegalArgumentException("Invalid course ID format: " + trimmed);
                }
            }
        }
    }

    private List<Course> fetchDistinctCourses(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }
        // Batch fetch all courses at once to avoid N+1 queries
        List<Long> distinctIds = courseIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Course> courseMap = courseRepository.findAllById(distinctIds).stream()
                .collect(java.util.stream.Collectors.toMap(Course::getId, course -> course, (a, _) -> a));

        // Verify all requested courses were found
        List<Long> missingIds = distinctIds.stream()
                .filter(id -> !courseMap.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException("Course(s) not found with id(s): " + missingIds);
        }

        // Return in the order requested (preserving order)
        return distinctIds.stream()
                .map(courseMap::get)
                .filter(Objects::nonNull)
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
        } catch (Exception _) {
            LOGGER.warn("Failed to create notification for mark addition, student ID: {}", studentId);
        }

        return studentMarkMapper.toDto(saved);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
        } catch (Exception _) {
            LOGGER.warn("Failed to create notification for mark update, student ID: {}", studentId);
        }

        return studentMarkMapper.toDto(saved);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public MarksCardDTO getMarksCard(Long studentId) {
        Student student = studentRepository.findByIdWithCourses(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
        List<StudentMark> marks = studentMarkRepository.findByStudentIdOrderByAssessedOnDesc(studentId);

        double totalScore = marks.stream()
                .map(StudentMark::getScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        double totalMax = marks.stream()
                .map(StudentMark::getMaxScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        Double percentage = totalMax > 0 ? (totalScore / totalMax) * PERCENTAGE_MULTIPLIER : null;
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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<StudentPerformanceDTO> getStudentPerformanceSummary() {
        List<StudentPerformanceDTO> summary = new ArrayList<>(studentRepository.fetchPerformanceSummary());
        summary.removeIf(dto -> dto.getTotalAssessments() <= 0);
        summary.sort(Comparator
                .comparingDouble(this::primaryScore)
                .reversed()
                .thenComparing(StudentPerformanceDTO::getStudentName, String.CASE_INSENSITIVE_ORDER));
        return summary;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void fixStudentLoginInfo(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        String emailLower = email.trim().toLowerCase();
        
        // Find student by email
        Student student = studentRepository.findAll().stream()
                .filter(s -> s.getEmail() != null && s.getEmail().trim().toLowerCase().equals(emailLower))
                .findFirst()
                .orElseThrow(() -> new StudentNotFoundException("Student not found with email: " + email));

        // Fix or create LoginInfo using the same pattern as createStudent
        LoginInfo loginInfo = createOrUpdateLoginInfoForStudent(emailLower, student.getName(), password);
        
        // Also update password in Student table
        if (password != null && !password.isBlank()) {
            String hashedPassword = passwordEncoder.encode(password.trim());
            student.setPassword(hashedPassword);
            studentRepository.save(student);
            LOGGER.info("Password also updated in Student table for student: {} ({})", student.getName(), email);
        } else if (loginInfo.getPassword() != null) {
            // Copy password from LoginInfo if no password provided
            student.setPassword(loginInfo.getPassword());
            studentRepository.save(student);
            LOGGER.info("Password copied from LoginInfo to Student table for student: {} ({})", student.getName(), email);
        }
        
        LOGGER.info("Fixed login info for student: {} ({}) - both LoginInfo and Student table updated", student.getName(), email);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public int syncPasswordsFromLoginInfo() {
        LOGGER.info("Starting password sync from LoginInfo to Student table...");
        int updated = 0;
        
        List<Student> students = studentRepository.findAll();
        for (Student student : students) {
            if (student.getEmail() == null || student.getEmail().isBlank()) {
                continue;
            }
            
            String email = student.getEmail().trim().toLowerCase();
            Optional<LoginInfo> loginInfo = loginInfoRepository.findByEmailIgnoreCase(email);
            
            if (loginInfo.isPresent() && loginInfo.get().getPassword() != null && !loginInfo.get().getPassword().isBlank()) {
                // Update Student password from LoginInfo if Student password is missing
                if (student.getPassword() == null || student.getPassword().isBlank()) {
                    student.setPassword(loginInfo.get().getPassword());
                    studentRepository.save(student);
                    updated++;
                    LOGGER.debug("Synced password for student: {} ({})", student.getName(), email);
                }
            }
        }
        
        LOGGER.info("Password sync completed. Updated {} students.", updated);
        return updated;
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
        double percentage = (score / maxScore) * PERCENTAGE_MULTIPLIER;
        List<GradeScale> scales = gradeScaleRepository.findAllByOrderByMinPercentageDesc();
        if (!scales.isEmpty()) {
            for (GradeScale scale : scales) {
                if (scale.getMinPercentage() != null) {
                    boolean meetsMin = percentage >= scale.getMinPercentage();
                    boolean belowMax = scale.getMaxPercentage() == null || percentage <= scale.getMaxPercentage();
                    if (meetsMin && belowMax) {
                        return scale.getGrade();
                    }
                }
            }
            // fall through to default chart if no matching rule
        }
        // Fallback default chart if no reference data configured
        if (percentage >= GRADE_OUTSTANDING_THRESHOLD) {
            return "Outstanding";
        } else if (percentage >= GRADE_A_PLUS_THRESHOLD) {
            return "A+";
        } else if (percentage >= GRADE_A_THRESHOLD) {
            return "A";
        } else if (percentage >= GRADE_B_PLUS_THRESHOLD) {
            return "B+";
        }
        return "B";
    }

    private List<String> extractUniqueCourseNames(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return List.of();
        }

        Map<String, String> unique = new LinkedHashMap<>();
        for (Course course : courses) {
            if (course != null) {
                String label = course.getName();
                if (label != null && !label.isBlank()) {
                    String key = course.getId() != null
                            ? "id:" + course.getId()
                            : "name:" + label.trim().toLowerCase(Locale.ROOT) + "|code:" +
                            (course.getCode() != null ? course.getCode().trim().toLowerCase(Locale.ROOT) : "");
                    unique.putIfAbsent(key, label.trim());
                }
            }
        }
        return new ArrayList<>(unique.values());
    }
}

