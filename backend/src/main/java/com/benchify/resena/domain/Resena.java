package com.benchify.resena.domain;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ForbiddenException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Valoración de 1 a 5 de un banco con comentario opcional. Al llegar a
 * {@link #UMBRAL_REPORTES} reportes de usuarios distintos pasa a la cola de moderación.
 *
 * <p>Los contadores de reportes solo los cambia la persistencia, con actualizaciones atómicas,
 * para que un reporte simultáneo a una edición no se pierda. {@code version} detecta dos
 * ediciones concurrentes de la misma reseña.
 */
public record Resena(
        UUID id,
        UUID bancoId,
        String autorId,
        String autorNombre,
        int puntuacion,
        String comentario,
        Instant fecha,
        Instant fechaEdicion,
        int numReportes,
        boolean reportada,
        long version) {

    public static final int MAX_COMENTARIO = 1000;
    public static final int UMBRAL_REPORTES = 3;

    public Resena {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bancoId, "bancoId");
        Objects.requireNonNull(autorId, "autorId");
        Objects.requireNonNull(fecha, "fecha");
        validarPuntuacion(puntuacion);
        comentario = limpiarComentario(comentario);
    }

    public static Resena publicar(UUID id, UUID bancoId, Actor actor, int puntuacion, String comentario,
            Instant ahora) {
        return new Resena(id, bancoId, actor.userId(), null, puntuacion, comentario, ahora, null, 0, false, 0);
    }

    /** Solo el autor puede editar su reseña. */
    public Resena editar(Actor actor, int nuevaPuntuacion, String nuevoComentario, Instant ahora) {
        if (!actor.userId().equals(autorId)) {
            throw new ForbiddenException("RESENA_NO_AUTOR", "Solo el autor puede editar su reseña");
        }
        return new Resena(id, bancoId, autorId, autorNombre, nuevaPuntuacion, nuevoComentario, fecha, ahora,
                numReportes, reportada, version);
    }

    /** El autor o un administrador pueden borrarla. */
    public void exigirBorradoPor(Actor actor) {
        if (!actor.canManage(autorId)) {
            throw new ForbiddenException("RESENA_NO_AUTOR", "No puedes borrar esta reseña");
        }
    }

    public void exigirReportablePor(Actor actor) {
        if (actor.userId().equals(autorId)) {
            throw new BusinessRuleException("RESENA_PROPIA", "No puedes reportar tu propia reseña");
        }
    }

    private static void validarPuntuacion(int puntuacion) {
        if (puntuacion < 1 || puntuacion > 5) {
            throw new BusinessRuleException("RESENA_PUNTUACION_INVALIDA", "La puntuación debe estar entre 1 y 5");
        }
    }

    private static String limpiarComentario(String comentario) {
        if (comentario == null || comentario.isBlank()) {
            return null;
        }
        String limpio = comentario.strip();
        if (limpio.length() > MAX_COMENTARIO) {
            throw new BusinessRuleException("RESENA_COMENTARIO_INVALIDO",
                    "El comentario no puede superar " + MAX_COMENTARIO + " caracteres");
        }
        return limpio;
    }
}
