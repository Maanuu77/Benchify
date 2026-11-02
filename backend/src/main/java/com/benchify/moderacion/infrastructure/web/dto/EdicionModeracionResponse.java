package com.benchify.moderacion.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Edición pendiente de un banco publicado.
 *
 * @param actual    lo que se ve ahora en el mapa
 * @param propuesta lo que quedaría al aprobarla
 */
public record EdicionModeracionResponse(UUID id, UUID bancoId, String autorId, String autorNombre, Instant fecha,
        DatosBancoResponse actual, DatosBancoResponse propuesta) {
}
