package com.studentdetails.details.Service;

import com.studentdetails.details.DTO.StudentProgressReportResponseDTO;

/**
 * Service interface for report generation operations.
 */
public interface ReportService {
    /**
     * Generates a student progress report for all students.
     *
     * @return the student progress report response DTO
     */
    StudentProgressReportResponseDTO generateStudentProgressReport();
}

