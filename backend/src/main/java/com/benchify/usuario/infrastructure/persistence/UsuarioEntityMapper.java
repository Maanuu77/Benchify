package com.benchify.usuario.infrastructure.persistence;

import com.benchify.usuario.domain.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface UsuarioEntityMapper {

    // conRol(Rol) y eliminar(Instant) son operaciones de dominio, no propiedades: MapStruct las tomaría
    // por setters fluidos
    @Mapping(target = "conRol", ignore = true)
    @Mapping(target = "eliminar", ignore = true)
    Usuario toDomain(UsuarioEntity entity);

    UsuarioEntity toEntity(Usuario usuario);
}
