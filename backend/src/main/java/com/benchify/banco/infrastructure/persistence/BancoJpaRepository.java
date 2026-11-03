package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.domain.EstadoBanco;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface BancoJpaRepository extends JpaRepository<BancoEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BancoEntity b where b.id = :id")
    Optional<BancoEntity> findByIdForUpdate(@Param("id") UUID id);

    Page<BancoEntity> findByCreadoPorAndEstadoIn(String creadoPor, Collection<EstadoBanco> estados, Pageable pageable);

    Page<BancoEntity> findByEstado(EstadoBanco estado, Pageable pageable);

    long countByEstado(EstadoBanco estado);

    /** Id y última modificación (verificación o, si no la hay, alta) de los bancos en un estado. */
    @Query("select b.id, coalesce(b.fechaVerificacion, b.fechaSubida) from BancoEntity b where b.estado = :estado "
            + "order by coalesce(b.fechaVerificacion, b.fechaSubida) desc, b.id")
    List<Object[]> findIdsYFechas(@Param("estado") EstadoBanco estado, Pageable pageable);

    long countByCreadoPorAndEstado(String creadoPor, EstadoBanco estado);
}
