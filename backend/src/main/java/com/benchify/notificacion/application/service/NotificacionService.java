package com.benchify.notificacion.application.service;

import com.benchify.notificacion.application.port.in.NotificacionesUseCase;
import com.benchify.notificacion.application.port.out.NotificacionRepository;
import com.benchify.notificacion.domain.Notificacion;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.NotFoundException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
public class NotificacionService implements NotificacionesUseCase {

    private final NotificacionRepository notificaciones;
    private final Clock clock;

    public NotificacionService(NotificacionRepository notificaciones, Clock clock) {
        this.notificaciones = notificaciones;
        this.clock = clock;
    }

    @Override
    public void notificar(String usuarioId, NuevaNotificacion nueva) {
        notificaciones.guardar(Notificacion.nueva(usuarioId, nueva.tipo(), nueva.titulo(), nueva.mensaje(),
                nueva.enlace(), clock.instant()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Notificacion> mias(Actor actor, PageQuery page) {
        return notificaciones.findByUsuario(actor.userId(), page);
    }

    @Override
    @Transactional(readOnly = true)
    public long noLeidas(Actor actor) {
        return notificaciones.contarNoLeidas(actor.userId());
    }

    @Override
    public void marcarLeida(Actor actor, UUID notificacionId) {
        Notificacion notificacion = notificaciones.findById(notificacionId)
                .orElseThrow(() -> new NotFoundException("NOTIFICACION_NOT_FOUND", "No existe la notificación " + notificacionId));
        notificacion.exigirDestinatario(actor);
        if (!notificacion.leida()) {
            notificaciones.marcarLeida(notificacionId);
        }
    }

    @Override
    public void marcarTodasLeidas(Actor actor) {
        notificaciones.marcarTodasLeidas(actor.userId());
    }
}
