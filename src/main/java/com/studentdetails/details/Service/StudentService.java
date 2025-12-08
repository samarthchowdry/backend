package com.studentdetails.details.Service;

import com.studentdetails.details.DTO.MarksCardDTO;
import com.studentdetails.details.DTO.StudentDTO;
import com.studentdetails.details.DTO.StudentMarkDTO;
import com.studentdetails.details.DTO.StudentPerformanceDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for student-related operations.
 * Methods in this interface are used by Spring Framework via dependency injection
 * and are called by REST controllers or other services.
 */
@SuppressWarnings("unused") // Suppress unused warnings - methods are used by Spring Framework
public interface StudentService {

    /**
     * Retrieves all students.
     * This method is called via REST API endpoints.
     *
     * @return list of student DTOs
     */
    List<StudentDTO> getAllStudents();

    /**
     * Retrieves a student by ID.
     * This method is called via REST API endpoints.
     *
     * @param id the student ID
     * @return the student DTO
     */
    StudentDTO getStudentById(Long id);

    /**
     * Creates a new student.
     * This method is called via REST API endpoints.
     *
     * @param studentDTO the student data
     * @return the created student DTO
     */
    StudentDTO createStudent(StudentDTO studentDTO);

    /**
     * Updates an existing student.
     * This method is called via REST API endpoints.
     *
     * @param id the student ID
     * @param studentDTO the updated student data
     * @return the updated student DTO
     */
    StudentDTO updateStudent(Long id, StudentDTO studentDTO);

    /**
     * Deletes a student by ID.
     * This method is called via REST API endpoints.
     *
     * @param id the student ID
     */
    void deleteStudent(Long id);

    /**
     * Counts the total number of students.
     * This method is called via REST API endpoints.
     *
     * @return the total count
     */
    long countStudents();

    /**
     * Retrieves filtered students based on criteria.
     * This method is called via REST API endpoints.
     *
     * @param name the student name filter
     * @param dateOfBirth the date of birth filter
     * @param email the email filter
     * @param branch the branch filter
     * @param id the student ID filter
     * @return list of filtered student DTOs
     */
    List<StudentDTO> getFilteredStudents(String name, LocalDate dateOfBirth, String email, String branch, Long id);

    /**
     * Bulk uploads students from a CSV file.
     * This method is called via REST API endpoints.
     *
     * @param file the CSV file
     * @return summary message
     */
    String bulkUploadStudents(org.springframework.web.multipart.MultipartFile file);

    /**
     * Bulk updates students from a CSV file.
     * This method is called via REST API endpoints.
     *
     * @param file the CSV file
     * @return summary message
     */
    String bulkUpdateStudents(org.springframework.web.multipart.MultipartFile file);

    /**
     * Adds a mark for a student.
     * This method is called via REST API endpoints.
     *
     * @param studentId the student ID
     * @param markDTO the mark data
     * @return the created mark DTO
     */
    StudentMarkDTO addMark(Long studentId, StudentMarkDTO markDTO);

    /**
     * Updates a mark for a student.
     * This method is called via REST API endpoints.
     *
     * @param studentId the student ID
     * @param markId the mark ID
     * @param markDTO the updated mark data
     * @return the updated mark DTO
     */
    StudentMarkDTO updateMark(Long studentId, Long markId, StudentMarkDTO markDTO);

    /**
     * Retrieves all marks for a student.
     * This method is called via REST API endpoints.
     *
     * @param studentId the student ID
     * @return list of mark DTOs
     */
    List<StudentMarkDTO> getMarks(Long studentId);

    /**
     * Retrieves a marks card for a student.
     * This method is called via REST API endpoints.
     *
     * @param studentId the student ID
     * @return the marks card DTO
     */
    MarksCardDTO getMarksCard(Long studentId);

    /**
     * Retrieves student performance summary.
     * This method is called via REST API endpoints.
     *
     * @return list of student performance DTOs
     */
    List<StudentPerformanceDTO> getStudentPerformanceSummary();

    /**
     * Fixes or creates LoginInfo for a student by email.
     * This is useful when a student was created but LoginInfo is missing or incorrect.
     *
     * @param email the student email
     * @param password optional password (if not provided, a random one will be generated)
     */
    void fixStudentLoginInfo(String email, String password);

    /**
     * Syncs passwords from LoginInfo to Student table for all students.
     * This is useful for backfilling passwords after adding the password column.
     *
     * @return number of students updated
     */
    int syncPasswordsFromLoginInfo();

}

