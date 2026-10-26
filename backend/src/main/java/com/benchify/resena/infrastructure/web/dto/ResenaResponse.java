package com.benchify.resena.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

/** Vista pública de una reseña; el número de reportes solo lo ve moderación. */
public record ResenaResponse(
        UUID id,
        UUID bancoId,
        String autorId,
        String autorNombre,
        int puntuacion,
        String comentario,
        Instant fecha,
        Instant fechaEdicion) {
}
