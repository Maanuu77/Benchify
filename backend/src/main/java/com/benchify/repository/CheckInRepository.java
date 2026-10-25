package com.benchify.repository;

import com.benchify.domain.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    List<CheckIn> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    long countByBancoId(UUID bancoId);

    long countByUsuarioId(Long usuarioId);
}
