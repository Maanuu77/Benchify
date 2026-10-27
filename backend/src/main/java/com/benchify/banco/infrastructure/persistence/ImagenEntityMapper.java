package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.domain.Imagen;
import org.mapstruct.Mapper;

@Mapper
interface ImagenEntityMapper {

    Imagen toDomain(ImagenEntity entity);

    ImagenEntity toEntity(Imagen imagen);
}
