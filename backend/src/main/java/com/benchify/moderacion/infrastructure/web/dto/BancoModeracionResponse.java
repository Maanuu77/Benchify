package com.benchify.moderacion.infrastructure.web.dto;

import com.benchify.banco.domain.EstadoBanco;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BancoModeracionResponse(
        UUID id,
        String titulo,
        String descripcion,
        double lat,
        double lng,
        String direccion,
        String ciudad,
        EstadoBanco estado,
        String autorId,
        String autorNombre,
        List<String> etiquetas,
        Instant fechaSubida) {
}
