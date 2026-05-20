package com.benchify.repository;

import com.benchify.domain.entity.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    Optional<Favorito> findByUsuarioIdAndBancoId(Long usuarioId, UUID bancoId);

    List<Favorito> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndBancoId(Long usuarioId, UUID bancoId);
}
