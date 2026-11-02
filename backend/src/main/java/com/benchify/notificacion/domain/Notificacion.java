package com.benchify.notificacion.domain;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.NotFoundException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aviso para un usuario, p. ej. que se ha publicado su banco.
 *
 * @param enlace ruta de la aplicación a la que lleva el aviso (como {@code /bancos/{id}}); puede faltar
 */
public record Notificacion(
        UUID id,
        String usuarioId,
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        String enlace,
        boolean leida,
        Instant fecha) {

    public static final int MAX_TITULO = 200;
    public static final int MAX_MENSAJE = 500;

    public Notificacion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(fecha, "fecha");
        titulo = recortar(titulo, MAX_TITULO);
        mensaje = mensaje == null || mensaje.isBlank() ? null : recortar(mensaje.strip(), MAX_MENSAJE);
    }

    public static Notificacion nueva(String usuarioId, TipoNotificacion tipo, String titulo, String mensaje,
            String enlace, Instant ahora) {
        return new Notificacion(UUID.randomUUID(), usuarioId, tipo, titulo, mensaje, enlace, false, ahora);
    }

    /** Cada usuario solo ve sus avisos: para los demás no existen. */
    public void exigirDestinatario(Actor actor) {
        if (!usuarioId.equals(actor.userId())) {
            throw new NotFoundException("NOTIFICACION_NOT_FOUND", "No existe la notificación " + id);
        }
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() > maximo ? texto.substring(0, maximo - 1) + "…" : texto;
    }
}
