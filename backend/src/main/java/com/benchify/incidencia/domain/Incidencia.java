package com.benchify.incidencia.domain;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aviso de un usuario sobre el estado de un banco. Lo revisa un moderador, que lo da por resuelto
 * (se ha arreglado, o el banco se ha retirado del mapa) o lo descarta.
 */
public record Incidencia(
        UUID id,
        UUID bancoId,
        String usuarioId,
        TipoIncidencia tipo,
        String comentario,
        EstadoIncidencia estado,
        Instant fecha,
        String resueltaPor,
        Instant fechaResolucion) {

    public static final int MAX_COMENTARIO = 500;

    public Incidencia {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bancoId, "bancoId");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(fecha, "fecha");
    }

    public static Incidencia reportar(UUID bancoId, Actor actor, TipoIncidencia tipo, String comentario, Instant ahora) {
        String limpio = comentario == null || comentario.isBlank() ? null : comentario.strip();
        if (limpio != null && limpio.length() > MAX_COMENTARIO) {
            throw new BusinessRuleException("INCIDENCIA_COMENTARIO_LARGO",
                    "El comentario no puede superar " + MAX_COMENTARIO + " caracteres");
        }
        if (tipo == TipoIncidencia.OTRO && limpio == null) {
            throw new BusinessRuleException("INCIDENCIA_SIN_DETALLE", "Cuéntanos qué le pasa al banco");
        }
        return new Incidencia(UUID.randomUUID(), bancoId, actor.userId(), tipo, limpio, EstadoIncidencia.ABIERTA,
                ahora, null, null);
    }

    /** Cierra la incidencia como resuelta o descartada. */
    public Incidencia cerrar(Actor moderador, EstadoIncidencia resultado, Instant ahora) {
        if (estado != EstadoIncidencia.ABIERTA) {
            throw new BusinessRuleException("INCIDENCIA_CERRADA", "La incidencia ya está cerrada");
        }
        if (resultado == EstadoIncidencia.ABIERTA) {
            throw new BusinessRuleException("INCIDENCIA_RESULTADO_INVALIDO", "Indica si está resuelta o descartada");
        }
        return new Incidencia(id, bancoId, usuarioId, tipo, comentario, resultado, fecha, moderador.userId(), ahora);
    }
}
