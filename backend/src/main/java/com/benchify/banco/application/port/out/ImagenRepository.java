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

    /**
     * Guarda y ejecuta el SQL en el acto: un error de la base de datos salta aquí y no al confirmar,
     * cuando ya no se podría reaccionar (p. ej. borrando los ficheros recién subidos).
     */
    Imagen saveAndFlush(Imagen imagen);

    Optional<Imagen> findById(UUID id);

    /**
     * Lee la imagen bloqueando su fila hasta el final de la transacción, para que dos moderaciones o
     * borrados simultáneos no se pisen.
     */
    Optional<Imagen> findByIdForUpdate(UUID id);

    void delete(UUID id);

    /** Imágenes no rechazadas de un banco, de la más antigua a la más reciente. */
    List<Imagen> findActivasByBanco(UUID bancoId);

    long countActivasByBanco(UUID bancoId);

    /** Imágenes en un estado, de la más antigua a la más reciente (cola de moderación). */
    PageResult<Imagen> findByEstado(EstadoImagen estado, PageQuery page);

    long countByEstado(EstadoImagen estado);
}
