package com.studentdetails.details.Mapper;

import com.studentdetails.details.DTO.StudentMarkDTO;
import com.studentdetails.details.Domain.StudentMark;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapper interface for converting between StudentMark entity and StudentMarkDTO.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StudentMarkMapper extends EntityMapper<StudentMarkDTO, StudentMark> {

    @Override
    @Mapping(target = "studentId", source = "student.id")
    StudentMarkDTO toDto(StudentMark entity);

    @Override
    @Mapping(target = "student", ignore = true)
    StudentMark toEntity(StudentMarkDTO dto);

}

