package com.benchify.banco.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

/** Hay una edición del banco esperando moderación, guardada en {@code fecha}. */
public record EdicionPendienteResponse(UUID id, Instant fecha) {
}
