package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.domain.EstadoBanco;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

interface BancoJpaRepository extends JpaRepository<BancoEntity, UUID> {

    boolean existsByIdAndEstado(UUID id, EstadoBanco estado);

    Page<BancoEntity> findByCreadoPorAndEstadoIn(String creadoPor, Collection<EstadoBanco> estados, Pageable pageable);

    Page<BancoEntity> findByEstado(EstadoBanco estado, Pageable pageable);

    long countByEstado(EstadoBanco estado);
}
