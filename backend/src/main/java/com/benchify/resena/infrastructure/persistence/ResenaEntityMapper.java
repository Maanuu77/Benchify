package com.benchify.resena.infrastructure.persistence;

import com.benchify.resena.domain.Resena;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface ResenaEntityMapper {

    Resena toDomain(ResenaEntity entity);

    @Mapping(target = "autorNombre", ignore = true)
    ResenaEntity toEntity(Resena resena);
}
