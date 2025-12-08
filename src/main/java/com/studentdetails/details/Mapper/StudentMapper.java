package com.studentdetails.details.Mapper;

import com.studentdetails.details.DTO.StudentDTO;
import com.studentdetails.details.Domain.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper interface for converting between Student entity and StudentDTO.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, uses = {StudentMarkMapper.class})
public interface StudentMapper extends EntityMapper<StudentDTO, Student> {

    @Override
    @Mapping(target = "courseIds", source = "courses", qualifiedByName = "mapCoursesToIds")
    @Mapping(target = "courseNames", source = "courses", qualifiedByName = "mapCoursesToNames")
    @Mapping(target = "marks", source = "marks")
    StudentDTO toDto(Student student);

    @Override
    @Mapping(target = "courses", ignore = true)
    @Mapping(target = "marks", ignore = true)
    @Mapping(target = "password", ignore = true) // Password is handled separately in service layer (hashing)
    Student toEntity(StudentDTO studentDTO);

    /**
     * Maps a list of courses to a list of course IDs.
     *
     * @param courses the list of courses
     * @return the list of course IDs
     */
    @Named("mapCoursesToIds")
    default List<Long> mapCoursesToIds(List<com.studentdetails.details.Domain.Course> courses) {
        if (courses == null) return new ArrayList<>();
        return courses.stream()
                .map(com.studentdetails.details.Domain.Course::getId)
                .distinct()
                .toList();
    }

    /**
     * Maps a list of courses to a list of course names.
     *
     * @param courses the list of courses
     * @return the list of course names
     */
    @Named("mapCoursesToNames")
    default List<String> mapCoursesToNames(List<com.studentdetails.details.Domain.Course> courses) {
        if (courses == null) return new ArrayList<>();
        return courses.stream()
                .map(com.studentdetails.details.Domain.Course::getName)
                .distinct()
                .toList();
    }
}
