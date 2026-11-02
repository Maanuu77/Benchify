package com.benchify.incidencia.infrastructure.web.dto;

import com.benchify.incidencia.domain.EstadoIncidencia;
import com.benchify.incidencia.domain.TipoIncidencia;

import java.time.Instant;
import java.util.UUID;

public record IncidenciaResponse(UUID id, UUID bancoId, String usuarioId, TipoIncidencia tipo, String comentario,
        EstadoIncidencia estado, Instant fecha) {
}
