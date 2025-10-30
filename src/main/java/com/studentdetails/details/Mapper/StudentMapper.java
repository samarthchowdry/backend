package com.studentdetails.details.Mapper;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.DTO.StudentDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentMapper extends EntityMapper<StudentDTO, Student> {
}
