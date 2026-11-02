package com.benchify.notificacion.infrastructure.web.dto;

import com.benchify.notificacion.domain.TipoNotificacion;

import java.time.Instant;
import java.util.UUID;

/** @param enlace ruta de la aplicación a la que lleva el aviso, p. ej. {@code /bancos/{id}} */
public record NotificacionResponse(UUID id, TipoNotificacion tipo, String titulo, String mensaje, String enlace,
        boolean leida, Instant fecha) {
}
