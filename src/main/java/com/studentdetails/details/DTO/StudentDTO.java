package com.studentdetails.details.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object for Student entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private Long id;
    private String name;
    private LocalDate dob;
    private String email;
    private String address;
    private String branch;
    /**
     * Optional plain text password for creating a login account.
     * If not provided, the system will generate a secure password.
     */
    private String password;
    private List<Long> courseIds;
    private List<String> courseNames;
    private List<StudentMarkDTO> marks;
}
