package com.benchify.notificacion.application.port.in;

import com.benchify.notificacion.domain.Notificacion;
import com.benchify.notificacion.domain.TipoNotificacion;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.UUID;

public interface NotificacionesUseCase {

    /** Avisa a un usuario; lo usan otras features (p. ej. moderación). */
    void notificar(String usuarioId, NuevaNotificacion notificacion);

    PageResult<Notificacion> mias(Actor actor, PageQuery page);

    long noLeidas(Actor actor);

    void marcarLeida(Actor actor, UUID notificacionId);

    void marcarTodasLeidas(Actor actor);

    record NuevaNotificacion(TipoNotificacion tipo, String titulo, String mensaje, String enlace) {
    }
}
