package com.benchify.repository;

import com.benchify.domain.entity.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findByBancoIdOrderByFechaResenaDesc(UUID bancoId);

    Optional<Resena> findByBancoIdAndUsuarioId(UUID bancoId, Long usuarioId);

    boolean existsByBancoIdAndUsuarioId(UUID bancoId, Long usuarioId);

    List<Resena> findByReportadaTrueOrderByNumReportesDesc();

    long countByBancoId(UUID bancoId);

    // Calcula la media de puntuación de un banco para desnormalizar en Banco.puntuacionMedia
    @Query("SELECT AVG(r.puntuacion) FROM Resena r WHERE r.banco.id = :bancoId")
    Double calcularMediaPuntuacion(@Param("bancoId") UUID bancoId);
}
