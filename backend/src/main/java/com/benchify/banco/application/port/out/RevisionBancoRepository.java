package com.benchify.banco.application.port.out;

import com.benchify.banco.domain.RevisionBanco;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface RevisionBancoRepository {

    /** Inserta una revisión nueva o actualiza una existente (mismo id). */
    void guardar(RevisionBanco revision);

    Optional<RevisionBanco> findById(UUID id);

    Optional<RevisionBanco> findPendienteByBanco(UUID bancoId);

    /** Pendientes de bancos no eliminados, de la más antigua a la más reciente (cola de moderación). */
    PageResult<RevisionBanco> findPendientes(PageQuery page);

    long countPendientes();
}
