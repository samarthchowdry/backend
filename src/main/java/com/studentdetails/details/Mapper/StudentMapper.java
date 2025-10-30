package com.studentdetails.details.Mapper;
import com.studentdetails.details.Domain.Student;
import com.studentdetails.details.DTO.StudentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StudentMapper extends EntityMapper<StudentDTO, Student> {

    @Override
    @Mapping(target = "courseIds", source = "courses", qualifiedByName = "mapCoursesToIds")
    @Mapping(target = "courseNames", source = "courses", qualifiedByName = "mapCoursesToNames")
    StudentDTO toDto(Student student);

    @Override
    @Mapping(target = "courses", ignore = true) // Ignore courses when converting from DTO to entity
    Student toEntity(StudentDTO studentDTO);

    @Named("mapCoursesToIds")
    default List<Long> mapCoursesToIds(List<com.studentdetails.details.Domain.Course> courses) {
        if (courses == null) return new ArrayList<>();
        return courses.stream()
                .map(com.studentdetails.details.Domain.Course::getId)
                .toList();
    }

    @Named("mapCoursesToNames")
    default List<String> mapCoursesToNames(List<com.studentdetails.details.Domain.Course> courses) {
        if (courses == null) return new ArrayList<>();
        return courses.stream()
                .map(com.studentdetails.details.Domain.Course::getName)
                .toList();
    }
}
