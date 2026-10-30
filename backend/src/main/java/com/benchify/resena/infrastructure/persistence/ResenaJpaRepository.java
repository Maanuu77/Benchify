package com.benchify.resena.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface ResenaJpaRepository extends JpaRepository<ResenaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResenaEntity r where r.id = :id")
    Optional<ResenaEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByBancoIdAndAutorId(UUID bancoId, String autorId);

    Page<ResenaEntity> findByBancoId(UUID bancoId, Pageable pageable);

    Page<ResenaEntity> findByAutorIdAndEstadoBanco(String autorId, String estadoBanco, Pageable pageable);

    Page<ResenaEntity> findByReportadaTrue(Pageable pageable);

    long countByReportadaTrue();

    /** JPQL (no SQL nativo) para que Hibernate vuelque antes los cambios pendientes de la transacción. */
    @Query("""
            select new com.benchify.resena.infrastructure.persistence.ResenaJpaRepository$Resumen(
                coalesce(avg(r.puntuacion), 0.0), count(r))
            from ResenaEntity r where r.bancoId = :bancoId
            """)
    Resumen resumen(@Param("bancoId") UUID bancoId);

    record Resumen(Double media, Long total) {
    }
}
