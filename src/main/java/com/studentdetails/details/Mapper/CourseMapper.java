package com.studentdetails.details.Mapper;

import com.studentdetails.details.DTO.CourseDTO;
import com.studentdetails.details.Domain.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper interface for converting between Course entity and CourseDTO.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CourseMapper extends EntityMapper<CourseDTO, Course> {

    @Override
    @Mapping(target = "studentIds", source = "students", qualifiedByName = "mapStudentsToIds")
    CourseDTO toDto(Course course);

    @Override
    @Mapping(target = "students", ignore = true)
    Course toEntity(CourseDTO courseDTO);

    /**
     * Maps a list of students to a list of student IDs.
     *
     * @param students the list of students
     * @return the list of student IDs
     */
    @Named("mapStudentsToIds")
    default List<Long> mapStudentsToIds(List<com.studentdetails.details.Domain.Student> students) {
        if (students == null) return new ArrayList<>();
        return students.stream()
                .map(com.studentdetails.details.Domain.Student::getId)
                .toList();
    }

}

