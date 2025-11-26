package com.studentdetails.details.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseSummaryDTO {
    private Long courseId;
    private String name;
    private String code;
    private Integer credits;
}

