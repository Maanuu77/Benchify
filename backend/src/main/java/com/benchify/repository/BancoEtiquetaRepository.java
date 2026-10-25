package com.benchify.repository;

import com.benchify.domain.entity.BancoEtiqueta;
import com.benchify.domain.entity.BancoEtiquetaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BancoEtiquetaRepository extends JpaRepository<BancoEtiqueta, BancoEtiquetaId> {

    List<BancoEtiqueta> findByBancoId(UUID bancoId);

    void deleteByBancoId(UUID bancoId);
}
