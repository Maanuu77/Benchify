package com.benchify.moderacion.infrastructure.web.dto;

import com.benchify.moderacion.domain.AccionModeracion;
import com.benchify.moderacion.domain.TipoRecurso;

import java.time.Instant;
import java.util.UUID;

public record EventoModeracionResponse(UUID id, String moderadorId, AccionModeracion accion, TipoRecurso recursoTipo,
        UUID recursoId, String motivo, Instant fecha) {
}
