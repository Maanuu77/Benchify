package com.benchify.incidencia.infrastructure.web.dto;

import com.benchify.incidencia.domain.TipoIncidencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidenciaRequest(
        @NotNull(message = "Indica qué le pasa al banco")
        TipoIncidencia tipo,
        @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
        String comentario) {
}
