package com.benchify.moderacion.infrastructure.web;

import com.benchify.banco.domain.Autor;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.Etiqueta;
import com.benchify.banco.domain.Imagen;
import com.benchify.moderacion.infrastructure.web.dto.BancoModeracionResponse;
import com.benchify.moderacion.infrastructure.web.dto.ImagenModeracionResponse;
import com.benchify.moderacion.infrastructure.web.dto.ResenaModeracionResponse;
import com.benchify.resena.domain.Resena;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper
interface ModeracionWebMapper {

    ResenaModeracionResponse toResponse(Resena resena);

    ImagenModeracionResponse toResponse(Imagen imagen);

    default BancoModeracionResponse toResponse(Banco banco) {
        return toResponse(banco.id(), banco.datos(), banco.estado(), banco.autor(), banco.fechaSubida());
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "titulo", source = "datos.titulo")
    @Mapping(target = "descripcion", source = "datos.descripcion")
    @Mapping(target = "lat", source = "datos.ubicacion.lat")
    @Mapping(target = "lng", source = "datos.ubicacion.lng")
    @Mapping(target = "direccion", source = "datos.direccion.direccion")
    @Mapping(target = "ciudad", source = "datos.direccion.ciudad")
    @Mapping(target = "autorId", source = "autor.id")
    @Mapping(target = "autorNombre", source = "autor.nombre")
    @Mapping(target = "etiquetas", source = "datos.etiquetas")
    BancoModeracionResponse toResponse(UUID id, DatosBanco datos, EstadoBanco estado, Autor autor,
            Instant fechaSubida);

    default List<String> nombres(Set<Etiqueta> etiquetas) {
        return etiquetas.stream().map(Etiqueta::nombre).sorted().toList();
    }
}
