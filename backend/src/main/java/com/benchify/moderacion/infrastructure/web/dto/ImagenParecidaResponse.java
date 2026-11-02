package com.benchify.moderacion.infrastructure.web.dto;

import com.benchify.banco.domain.EstadoImagen;

import java.util.UUID;

/**
 * Foto ya subida que se parece a una pendiente. Las rechazadas no tienen fichero: {@code thumbUrl}
 * es null.
 *
 * @param mismoBanco si es del mismo banco que la pendiente (lo más probable es que sea repetida)
 */
public record ImagenParecidaResponse(UUID id, UUID bancoId, String thumbUrl, boolean requiereAutenticacion,
        EstadoImagen estado, boolean mismoBanco) {
}
