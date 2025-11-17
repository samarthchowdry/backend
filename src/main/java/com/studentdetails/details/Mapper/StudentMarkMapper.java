package com.studentdetails.details.Mapper;

import com.studentdetails.details.DTO.StudentMarkDTO;
import com.studentdetails.details.Domain.StudentMark;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StudentMarkMapper extends EntityMapper<StudentMarkDTO, StudentMark> {

    @Override
    @BeanMapping(ignoreByDefault = false)
    @Mapping(target = "studentId", source = "student.id")
    StudentMarkDTO toDto(StudentMark entity);

    @Override
    @Mapping(target = "student", ignore = true)
    StudentMark toEntity(StudentMarkDTO dto);

}

