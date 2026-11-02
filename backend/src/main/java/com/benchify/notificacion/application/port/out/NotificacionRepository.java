package com.benchify.notificacion.application.port.out;

import com.benchify.notificacion.domain.Notificacion;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface NotificacionRepository {

    void guardar(Notificacion notificacion);

    Optional<Notificacion> findById(UUID id);

    /** Las del usuario, de la más reciente a la más antigua. */
    PageResult<Notificacion> findByUsuario(String usuarioId, PageQuery page);

    long contarNoLeidas(String usuarioId);

    void marcarLeida(UUID id);

    void marcarTodasLeidas(String usuarioId);
}
