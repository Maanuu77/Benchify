package com.benchify.checkin.application.port.out;

import com.benchify.checkin.domain.CheckIn;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository {

    CheckIn save(CheckIn checkIn);

    /**
     * Fecha del último check-in del usuario en el banco. Antes bloquea la pareja (usuario, banco)
     * hasta el final de la transacción: dos peticiones simultáneas no pueden pasar a la vez la
     * comprobación del intervalo mínimo y guardar las dos.
     */
    Optional<Instant> findUltimaFechaForUpdate(String usuarioId, UUID bancoId);

    /** Más recientes primero. */
    PageResult<CheckIn> findByUsuario(String usuarioId, PageQuery page);

    long countByBanco(UUID bancoId);
}
