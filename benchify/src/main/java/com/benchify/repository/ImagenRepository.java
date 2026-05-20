package com.benchify.repository;

import com.benchify.domain.entity.Imagen;
import com.benchify.domain.enums.EstadoModeracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImagenRepository extends JpaRepository<Imagen, Long> {

    List<Imagen> findByBancoIdAndEstadoModeracion(UUID bancoId, EstadoModeracion estadoModeracion);

    List<Imagen> findByEstadoModeracionOrderByFechaSubidaDesc(EstadoModeracion estadoModeracion);

    boolean existsByHashPerceptual(String hashPerceptual);
}
