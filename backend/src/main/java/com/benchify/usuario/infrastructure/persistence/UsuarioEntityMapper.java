package com.benchify.usuario.infrastructure.persistence;

import com.benchify.usuario.domain.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface UsuarioEntityMapper {

    // conRol(Rol) es una operación de dominio, no una propiedad: MapStruct lo tomaría por un setter fluido
    @Mapping(target = "conRol", ignore = true)
    Usuario toDomain(UsuarioEntity entity);

    UsuarioEntity toEntity(Usuario usuario);
}
