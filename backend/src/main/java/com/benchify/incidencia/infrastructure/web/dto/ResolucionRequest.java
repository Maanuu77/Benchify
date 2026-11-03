package com.benchify.incidencia.infrastructure.web.dto;

import com.benchify.incidencia.domain.EstadoIncidencia;
import jakarta.validation.constraints.NotNull;

/** @param estado RESUELTA o DESCARTADA */
public record ResolucionRequest(@NotNull(message = "Indica si está resuelta o descartada") EstadoIncidencia estado) {
}
