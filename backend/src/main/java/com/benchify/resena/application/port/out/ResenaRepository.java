package com.benchify.resena.application.port.out;

import com.benchify.resena.domain.Resena;
import com.benchify.resena.domain.ResumenPuntuacion;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ResenaRepository {

    /**
     * Inserta una reseña nueva o guarda la edición de una existente (puntuación, comentario y fecha
     * de edición; nunca los contadores de reportes). Si la reseña ha cambiado desde que se leyó
     * ({@link Resena#version()}) lanza un fallo de bloqueo optimista en lugar de pisar el cambio.
     */
    Resena save(Resena resena);

    Optional<Resena> findById(UUID id);

    /** Bloquea la fila hasta el final de la transacción para serializar reportes y moderación. */
    Optional<Resena> findByIdForUpdate(UUID id);

    void delete(UUID id);

    boolean existsByBancoAndAutor(UUID bancoId, String autorId);

    /** Hay como mucho una reseña por usuario y banco. */
    Optional<Resena> findByBancoAndAutor(UUID bancoId, String autorId);

    /** Más recientes primero. */
    PageResult<Resena> findByBanco(UUID bancoId, PageQuery page);

    /** Reseñas de un autor en bancos publicados, las más recientes primero. */
    PageResult<Resena> findByAutorEnBancosPublicados(String autorId, PageQuery page);

    /** Cola de moderación: las más reportadas primero. */
    PageResult<Resena> findReportadas(PageQuery page);

    long countReportadas();

    long count();

    /** @return {@code false} si ese usuario ya había reportado la reseña */
    boolean registrarReporte(UUID resenaId, String usuarioId, Instant fecha);

    /**
     * Suma un reporte con un UPDATE atómico ({@code num_reportes = num_reportes + 1}) y marca la
     * reseña para moderación al llegar a {@code umbral}.
     *
     * @return la reseña con los contadores ya actualizados
     */
    Resena sumarReporte(UUID resenaId, int umbral);

    /**
     * Borra los reportes de la reseña y pone sus contadores a cero.
     *
     * @return la reseña ya sin reportes
     */
    Resena borrarReportes(UUID resenaId);

    ResumenPuntuacion resumen(UUID bancoId);
}
