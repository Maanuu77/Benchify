package com.benchify.banco.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface EtiquetaJpaRepository extends JpaRepository<EtiquetaEntity, Long> {

    List<EtiquetaEntity> findAllByOrderByNombreAsc();
}
