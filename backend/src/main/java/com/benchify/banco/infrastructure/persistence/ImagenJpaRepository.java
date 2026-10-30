package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.domain.EstadoImagen;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ImagenJpaRepository extends JpaRepository<ImagenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ImagenEntity i where i.id = :id")
    Optional<ImagenEntity> findByIdForUpdate(@Param("id") UUID id);

    List<ImagenEntity> findByBancoIdAndEstadoNotOrderByFechaSubidaAscIdAsc(UUID bancoId, EstadoImagen estado);

    long countByBancoIdAndEstadoNot(UUID bancoId, EstadoImagen estado);

    Page<ImagenEntity> findByEstado(EstadoImagen estado, Pageable pageable);

    long countByEstado(EstadoImagen estado);
}
