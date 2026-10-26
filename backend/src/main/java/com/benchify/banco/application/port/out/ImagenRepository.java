package com.benchify.banco.application.port.out;

import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImagenRepository {

    Imagen save(Imagen imagen);

    Optional<Imagen> findById(UUID id);

    void delete(UUID id);

    /** Imágenes no rechazadas de un banco, de la más antigua a la más reciente. */
    List<Imagen> findActivasByBanco(UUID bancoId);

    long countActivasByBanco(UUID bancoId);

    /** Imágenes en un estado, de la más antigua a la más reciente (cola de moderación). */
    PageResult<Imagen> findByEstado(EstadoImagen estado, PageQuery page);

    long countByEstado(EstadoImagen estado);
}
