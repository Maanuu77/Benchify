package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.domain.EstadoImagen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ImagenJpaRepository extends JpaRepository<ImagenEntity, UUID> {

    List<ImagenEntity> findByBancoIdAndEstadoNotOrderByFechaSubidaAscIdAsc(UUID bancoId, EstadoImagen estado);

    long countByBancoIdAndEstadoNot(UUID bancoId, EstadoImagen estado);

    Page<ImagenEntity> findByEstado(EstadoImagen estado, Pageable pageable);

    long countByEstado(EstadoImagen estado);
}
