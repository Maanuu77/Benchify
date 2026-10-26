package com.benchify.moderacion.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ResenaModeracionResponse(
        UUID id,
        UUID bancoId,
        String autorId,
        String autorNombre,
        int puntuacion,
        String comentario,
        int numReportes,
        Instant fecha) {
}
