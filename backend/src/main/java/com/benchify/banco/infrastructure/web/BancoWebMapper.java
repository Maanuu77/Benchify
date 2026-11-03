package com.benchify.banco.infrastructure.web;

import com.benchify.banco.application.port.in.BancoCommand;
import com.benchify.banco.domain.Autor;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoCercano;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.Etiqueta;
import com.benchify.banco.domain.Imagen;
import com.benchify.banco.domain.Valoracion;
import com.benchify.banco.infrastructure.web.dto.BancoCercanoResponse;
import com.benchify.banco.infrastructure.web.dto.BancoRequest;
import com.benchify.banco.infrastructure.web.dto.BancoResponse;
import com.benchify.banco.infrastructure.web.dto.BancoResumenResponse;
import com.benchify.banco.infrastructure.web.dto.EtiquetaResponse;
import com.benchify.banco.infrastructure.web.dto.ImagenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@link Banco} expone su estado con accesores de estilo record; los métodos {@code default}
 * lo descomponen y MapStruct mapea desde los value objects (varias fuentes por método).
 */
@Mapper
interface BancoWebMapper {

    BancoCommand toCommand(BancoRequest request);

    EtiquetaResponse toResponse(Etiqueta etiqueta);

    List<EtiquetaResponse> toEtiquetaResponses(List<Etiqueta> etiquetas);

    default ImagenResponse toResponse(Imagen imagen) {
        return new ImagenResponse(imagen.id(), imagen.bancoId(), ImagenUrls.foto(imagen), ImagenUrls.miniatura(imagen),
                ImagenUrls.requiereAutenticacion(imagen), imagen.estado(), imagen.subidaPor(), imagen.fechaSubida());
    }

    default BancoResponse toResponse(Banco banco) {
        return toResponse(banco.id(), banco.datos(), banco.estado(), banco.autor(), banco.valoracion(),
                banco.fechaSubida(), banco.fechaVerificacion());
    }

    default BancoResumenResponse toResumen(Banco banco) {
        return toResumen(banco.id(), banco.datos(), banco.estado(), banco.valoracion());
    }

    default BancoCercanoResponse toResponse(BancoCercano cercano) {
        return new BancoCercanoResponse(toResumen(cercano.banco()), Math.round(cercano.distanciaMetros()));
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "titulo", source = "datos.titulo")
    @Mapping(target = "descripcion", source = "datos.descripcion")
    @Mapping(target = "lat", source = "datos.ubicacion.lat")
    @Mapping(target = "lng", source = "datos.ubicacion.lng")
    @Mapping(target = "direccion", source = "datos.direccion.direccion")
    @Mapping(target = "ciudad", source = "datos.direccion.ciudad")
    @Mapping(target = "provincia", source = "datos.direccion.provincia")
    @Mapping(target = "pais", source = "datos.direccion.pais")
    @Mapping(target = "conSombra", source = "datos.caracteristicas.conSombra")
    @Mapping(target = "accesible", source = "datos.caracteristicas.accesible")
    @Mapping(target = "conRespaldo", source = "datos.caracteristicas.conRespaldo")
    @Mapping(target = "material", source = "datos.caracteristicas.material")
    @Mapping(target = "puntuacionMedia", source = "valoracion.media")
    @Mapping(target = "numResenas", source = "valoracion.numResenas")
    @Mapping(target = "etiquetas", source = "datos.etiquetas")
    BancoResponse toResponse(UUID id, DatosBanco datos, EstadoBanco estado, Autor autor, Valoracion valoracion,
            Instant fechaSubida, Instant fechaVerificacion);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "titulo", source = "datos.titulo")
    @Mapping(target = "lat", source = "datos.ubicacion.lat")
    @Mapping(target = "lng", source = "datos.ubicacion.lng")
    @Mapping(target = "ciudad", source = "datos.direccion.ciudad")
    @Mapping(target = "accesible", source = "datos.caracteristicas.accesible")
    @Mapping(target = "conSombra", source = "datos.caracteristicas.conSombra")
    @Mapping(target = "conRespaldo", source = "datos.caracteristicas.conRespaldo")
    @Mapping(target = "puntuacionMedia", source = "valoracion.media")
    @Mapping(target = "numResenas", source = "valoracion.numResenas")
    @Mapping(target = "etiquetas", source = "datos.etiquetas")
    BancoResumenResponse toResumen(UUID id, DatosBanco datos, EstadoBanco estado, Valoracion valoracion);

    /** Etiquetas en orden alfabético para que la respuesta sea estable. */
    default List<EtiquetaResponse> etiquetasOrdenadas(Set<Etiqueta> etiquetas) {
        return toEtiquetaResponses(etiquetas.stream().sorted(Comparator.comparing(Etiqueta::nombre)).toList());
    }

    default List<String> nombresEtiquetas(Set<Etiqueta> etiquetas) {
        return etiquetas.stream().map(Etiqueta::nombre).sorted().toList();
    }
}
