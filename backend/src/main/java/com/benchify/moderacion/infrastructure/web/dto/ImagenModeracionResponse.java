package com.benchify.moderacion.infrastructure.web.dto;

import com.benchify.banco.domain.EstadoImagen;

import java.time.Instant;
import java.util.UUID;

/** @param thumbUrl miniatura para la cola de revisión; en las fotos antiguas, la misma {@code url} */
public record ImagenModeracionResponse(UUID id, UUID bancoId, String url, String thumbUrl, EstadoImagen estado,
        String subidaPor, long tamanoBytes, Instant fechaSubida) {
}
