package com.benchify.moderacion.application.port.out;

import com.benchify.moderacion.domain.EventoModeracion;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

public interface EventoModeracionRepository {

    void guardar(EventoModeracion evento);

    /** Historial, del más reciente al más antiguo. */
    PageResult<EventoModeracion> findRecientes(PageQuery page);
}
