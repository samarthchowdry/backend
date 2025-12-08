package com.studentdetails.details.DTO;

import lombok.*;

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

