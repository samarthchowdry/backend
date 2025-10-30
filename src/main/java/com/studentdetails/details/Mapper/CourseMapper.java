package com.studentdetails.details.Mapper;

import com.studentdetails.details.Domain.Course;
import com.studentdetails.details.DTO.CourseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CourseMapper extends EntityMapper<CourseDTO, Course> {

    @Override
    @Mapping(target = "studentIds", source = "students", qualifiedByName = "mapStudentsToIds")
   // @Mapping(target = "studentNames", source = "students", qualifiedByName = "mapStudentsToNames")
    CourseDTO toDto(Course course);

    @Override
    @Mapping(target = "students", ignore = true) // Ignore students list when converting from DTO
    Course toEntity(CourseDTO courseDTO);

    @Named("mapStudentsToIds")
    default List<Long> mapStudentsToIds(List<com.studentdetails.details.Domain.Student> students) {
        if (students == null) return new ArrayList<>();
        return students.stream()
                .map(com.studentdetails.details.Domain.Student::getId)
                .toList();
    }

    @Named("mapStudentsToNames")
    default List<String> mapStudentsToNames(List<com.studentdetails.details.Domain.Student> students) {
        if (students == null) return new ArrayList<>();
        return students.stream()
                .map(com.studentdetails.details.Domain.Student::getName)
                .toList();
    }
}

