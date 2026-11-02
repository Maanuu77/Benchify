package com.benchify.moderacion.infrastructure.web.dto;

import com.benchify.banco.domain.MaterialBanco;

import java.util.List;

/** Todos los campos editables de un banco, para comparar la versión publicada con la propuesta. */
public record DatosBancoResponse(
        String titulo,
        String descripcion,
        double lat,
        double lng,
        String direccion,
        String ciudad,
        String provincia,
        String pais,
        boolean conSombra,
        boolean accesible,
        boolean conRespaldo,
        MaterialBanco material,
        List<String> etiquetas) {
}
