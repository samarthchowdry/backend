package com.studentdetails.details.Mapper;

import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.DTO.StudentDTO;
import java.time.LocalDate;

public class StudentMapper {

    public static StudentDTO toDTO(Student student) {
        return new StudentDTO(
                student.getId(),
                student.getName(),
                student.getDob().toString()
        );
    }

    public static Student toEntity(StudentDTO dto) {
        return new Student(
                null, // ID will be auto-generated
                dto.getName(),
                LocalDate.parse(dto.getDob())
        );
    }
}
