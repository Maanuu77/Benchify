package com.benchify.repository;

import com.benchify.domain.entity.Etiqueta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {

    Optional<Etiqueta> findByNombre(String nombre);

    List<Etiqueta> findAllByOrderByNombreAsc();

    boolean existsByNombre(String nombre);
}
