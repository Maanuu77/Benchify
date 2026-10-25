package com.benchify.banco.application.port.in;

import com.benchify.banco.domain.MaterialBanco;

import java.util.Set;

/** Datos que envía un usuario al proponer o editar un banco. */
public record BancoCommand(
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
        Set<Long> etiquetaIds) {
}
