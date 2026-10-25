package com.benchify.banco.infrastructure.web.dto;

import com.benchify.banco.domain.EstadoBanco;

import java.util.List;
import java.util.UUID;

/** Versión ligera para marcadores del mapa y listados. */
public record BancoResumenResponse(
        UUID id,
        String titulo,
        double lat,
        double lng,
        String ciudad,
        EstadoBanco estado,
        double puntuacionMedia,
        int numResenas,
        boolean accesible,
        boolean conSombra,
        boolean conRespaldo,
        List<String> etiquetas) {
}
