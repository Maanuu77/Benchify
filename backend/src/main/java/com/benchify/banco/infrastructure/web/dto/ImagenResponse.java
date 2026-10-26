package com.benchify.banco.infrastructure.web.dto;

import com.benchify.banco.domain.EstadoImagen;

import java.time.Instant;
import java.util.UUID;

public record ImagenResponse(UUID id, UUID bancoId, String url, EstadoImagen estado, String subidaPor,
        Instant fechaSubida) {
}
