package com.studentdetails.details.Mapper;

import java.util.List;

/**
 * Generic mapper interface for converting between DTOs and entities.
 *
 * @param <D> the DTO type
 * @param <E> the entity type
 */
public interface EntityMapper<D, E> {

    D toDto(E entity);

    E toEntity(D dto);

    List<D> toDto(List<E> entityList);

}
