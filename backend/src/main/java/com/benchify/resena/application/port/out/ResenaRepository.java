package com.benchify.resena.application.port.out;

import com.benchify.resena.domain.Resena;
import com.benchify.resena.domain.ResumenPuntuacion;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ResenaRepository {

    Resena save(Resena resena);

    Optional<Resena> findById(UUID id);

    /** Bloquea la fila para contar reportes concurrentes sin perder ninguno. */
    Optional<Resena> findByIdForUpdate(UUID id);

    void delete(UUID id);

    boolean existsByBancoAndAutor(UUID bancoId, String autorId);

    /** Más recientes primero. */
    PageResult<Resena> findByBanco(UUID bancoId, PageQuery page);

    /** Más recientes primero. */
    PageResult<Resena> findByAutor(String autorId, PageQuery page);

    /** Cola de moderación: las más reportadas primero. */
    PageResult<Resena> findReportadas(PageQuery page);

    long countReportadas();

    long count();

    /** @return {@code false} si ese usuario ya había reportado la reseña */
    boolean registrarReporte(UUID resenaId, String usuarioId, Instant fecha);

    void borrarReportes(UUID resenaId);

    ResumenPuntuacion resumen(UUID bancoId);
}
