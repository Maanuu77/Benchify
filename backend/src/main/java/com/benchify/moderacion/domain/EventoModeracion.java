package com.benchify.moderacion.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Una decisión de moderación, para poder revisar después quién hizo qué y por qué.
 *
 * @param motivo explicación opcional del moderador; también se envía al autor del contenido
 */
public record EventoModeracion(
        UUID id,
        String moderadorId,
        AccionModeracion accion,
        UUID recursoId,
        String motivo,
        Instant fecha) {

    public static final int MAX_MOTIVO = 500;

    public EventoModeracion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(moderadorId, "moderadorId");
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(recursoId, "recursoId");
        Objects.requireNonNull(fecha, "fecha");
        motivo = limpiarMotivo(motivo);
    }

    public static EventoModeracion registrar(String moderadorId, AccionModeracion accion, UUID recursoId,
            String motivo, Instant ahora) {
        return new EventoModeracion(UUID.randomUUID(), moderadorId, accion, recursoId, motivo, ahora);
    }

    public TipoRecurso recursoTipo() {
        return accion.recurso();
    }

    /** Sin espacios sobrantes y como mucho {@link #MAX_MOTIVO} caracteres; vacío equivale a no darlo. */
    public static String limpiarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return null;
        }
        String limpio = motivo.strip();
        return limpio.length() > MAX_MOTIVO ? limpio.substring(0, MAX_MOTIVO) : limpio;
    }
}
