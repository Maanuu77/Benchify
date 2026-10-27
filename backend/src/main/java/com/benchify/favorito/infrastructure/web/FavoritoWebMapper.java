package com.benchify.favorito.infrastructure.web;

import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.Valoracion;
import com.benchify.favorito.application.port.in.FavoritoConBanco;
import com.benchify.favorito.domain.Favorito;
import com.benchify.favorito.infrastructure.web.dto.FavoritoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface FavoritoWebMapper {

    default FavoritoResponse toResponse(FavoritoConBanco item) {
        return toResponse(item.favorito(), item.banco().datos(), item.banco().valoracion());
    }

    @Mapping(target = "bancoId", source = "favorito.bancoId")
    @Mapping(target = "fecha", source = "favorito.fecha")
    @Mapping(target = "titulo", source = "datos.titulo")
    @Mapping(target = "lat", source = "datos.ubicacion.lat")
    @Mapping(target = "lng", source = "datos.ubicacion.lng")
    @Mapping(target = "ciudad", source = "datos.direccion.ciudad")
    @Mapping(target = "puntuacionMedia", source = "valoracion.media")
    @Mapping(target = "numResenas", source = "valoracion.numResenas")
    FavoritoResponse toResponse(Favorito favorito, DatosBanco datos, Valoracion valoracion);
}
