package com.benchify.incidencia.infrastructure.web.dto;

import com.benchify.incidencia.domain.TipoIncidencia;

/** Avisos abiertos de un tipo en un banco. */
public record ResumenIncidenciasResponse(TipoIncidencia tipo, long total) {
}
