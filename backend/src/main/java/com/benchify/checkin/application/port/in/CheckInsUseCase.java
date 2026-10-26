package com.benchify.checkin.application.port.in;

import com.benchify.checkin.domain.CheckIn;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Ubicacion;

import java.util.UUID;

public interface CheckInsUseCase {

    /** @param posicion dónde dice el dispositivo del usuario que está */
    CheckIn registrar(Actor actor, UUID bancoId, Ubicacion posicion);

    /** Check-ins del actor, los más recientes primero, con el título del banco. */
    PageResult<CheckInConBanco> misCheckIns(Actor actor, PageQuery page);

    long totalDeBanco(UUID bancoId);
}
