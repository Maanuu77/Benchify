package com.benchify.banco.infrastructure.web.dto;

import com.benchify.banco.domain.EstadoImagen;

import java.time.Instant;
import java.util.UUID;

/**
 * @param url      foto completa (lado mayor de 2048 px como mucho)
 * @param thumbUrl miniatura para galerías; en las fotos antiguas, la misma {@code url}
 */
public record ImagenResponse(UUID id, UUID bancoId, String url, String thumbUrl, EstadoImagen estado,
        String subidaPor, Instant fechaSubida) {
}
