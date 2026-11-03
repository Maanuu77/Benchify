package com.benchify.incidencia.application.port.out;

import com.benchify.incidencia.domain.Incidencia;
import com.benchify.incidencia.domain.TipoIncidencia;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IncidenciaRepository {

    void guardar(Incidencia incidencia);

    /** Cambia el estado de una incidencia ya guardada. */
    void actualizar(Incidencia incidencia);

    Optional<Incidencia> findById(UUID id);

    boolean existeAbierta(UUID bancoId, String usuarioId);

    Map<TipoIncidencia, Long> contarAbiertasPorTipo(UUID bancoId);

    PageResult<Incidencia> findAbiertas(PageQuery page);

    long contarAbiertas();
}
