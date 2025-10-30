package com.studentdetails.details.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer credits;
    private List<Long> studentIds; // List of student IDs
    //private List<String> studentNames; // List of student names for display
}

