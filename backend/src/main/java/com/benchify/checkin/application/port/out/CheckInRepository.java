package com.benchify.checkin.application.port.out;

import com.benchify.checkin.domain.CheckIn;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository {

    CheckIn save(CheckIn checkIn);

    Optional<Instant> findUltimaFecha(String usuarioId, UUID bancoId);

    /** Más recientes primero. */
    PageResult<CheckIn> findByUsuario(String usuarioId, PageQuery page);

    long countByBanco(UUID bancoId);
}
