package com.benchify.banco.infrastructure.web.dto;

import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.MaterialBanco;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Ficha completa de un banco. */
public record BancoResponse(
        UUID id,
        String titulo,
        String descripcion,
        double lat,
        double lng,
        String direccion,
        String ciudad,
        String provincia,
        String pais,
        EstadoBanco estado,
        boolean conSombra,
        boolean accesible,
        boolean conRespaldo,
        MaterialBanco material,
        double puntuacionMedia,
        int numResenas,
        AutorResponse autor,
        List<EtiquetaResponse> etiquetas,
        Instant fechaSubida,
        Instant fechaVerificacion) {
}
