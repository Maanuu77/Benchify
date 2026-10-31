package com.benchify.resena.infrastructure.persistence;

import com.benchify.resena.domain.Resena;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface ResenaEntityMapper {

    Resena toDomain(ResenaEntity entity);

    @Mapping(target = "autorNombre", ignore = true)
    @Mapping(target = "estadoBanco", ignore = true)
    ResenaEntity toEntity(Resena resena);

    /** Lo único que cambia al editar; contadores y versión los gestionan el adaptador e Hibernate. */
    default void copiarEdicion(Resena resena, ResenaEntity entity) {
        entity.setPuntuacion(resena.puntuacion());
        entity.setComentario(resena.comentario());
        entity.setFechaEdicion(resena.fechaEdicion());
    }
}
