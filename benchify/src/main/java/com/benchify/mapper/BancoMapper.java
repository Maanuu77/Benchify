package com.benchify.mapper;

import com.benchify.domain.entity.Banco;
import com.benchify.domain.entity.Imagen;
import com.benchify.domain.enums.EstadoModeracion;
import com.benchify.dto.BancoMapaResponse;
import com.benchify.dto.BancoResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BancoMapper {

    public BancoResponse toResponse(Banco banco) {
        List<String> etiquetas = banco.getBancoEtiquetas().stream()
                .map(be -> be.getEtiqueta().getNombre())
                .toList();

        List<String> imagenes = banco.getImagenes().stream()
                .filter(img -> img.getEstadoModeracion() == EstadoModeracion.APROBADA)
                .map(Imagen::getUrlR2)
                .toList();

        return BancoResponse.builder()
                .id(banco.getId())
                .titulo(banco.getTitulo())
                .descripcion(banco.getDescripcion())
                .lat(banco.getLat())
                .lng(banco.getLng())
                .direccion(banco.getDireccion())
                .ciudad(banco.getCiudad())
                .provincia(banco.getProvincia())
                .pais(banco.getPais())
                .estado(banco.getEstado())
                .conSombra(banco.isConSombra())
                .accesible(banco.isAccesible())
                .conRespaldo(banco.isConRespaldo())
                .material(banco.getMaterial())
                .puntuacionMedia(banco.getPuntuacionMedia())
                .numResenas(banco.getNumResenas())
                .fechaSubida(banco.getFechaSubida())
                .creadoPorNombre(banco.getCreadoPor().getNombre())
                .etiquetas(etiquetas)
                .imagenesUrls(imagenes)
                .build();
    }

    public BancoMapaResponse toMapaResponse(Banco banco) {
        List<String> etiquetas = banco.getBancoEtiquetas().stream()
                .map(be -> be.getEtiqueta().getNombre())
                .toList();

        String primeraImagen = banco.getImagenes().stream()
                .filter(img -> img.getEstadoModeracion() == EstadoModeracion.APROBADA)
                .map(Imagen::getUrlR2)
                .findFirst()
                .orElse(null);

        return BancoMapaResponse.builder()
                .id(banco.getId())
                .titulo(banco.getTitulo())
                .lat(banco.getLat())
                .lng(banco.getLng())
                .puntuacionMedia(banco.getPuntuacionMedia())
                .numResenas(banco.getNumResenas())
                .accesible(banco.isAccesible())
                .conSombra(banco.isConSombra())
                .etiquetas(etiquetas)
                .primeraImagenUrl(primeraImagen)
                .build();
    }
}
