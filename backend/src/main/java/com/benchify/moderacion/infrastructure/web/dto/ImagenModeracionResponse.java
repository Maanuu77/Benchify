package com.benchify.moderacion.infrastructure.web.dto;

import com.benchify.banco.domain.EstadoImagen;

import java.time.Instant;
import java.util.UUID;

public record ImagenModeracionResponse(UUID id, UUID bancoId, String url, EstadoImagen estado, String subidaPor,
        long tamanoBytes, Instant fechaSubida) {
}
