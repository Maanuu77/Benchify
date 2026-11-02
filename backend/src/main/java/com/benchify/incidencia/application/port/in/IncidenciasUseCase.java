package com.benchify.incidencia.application.port.in;

import com.benchify.incidencia.domain.EstadoIncidencia;
import com.benchify.incidencia.domain.Incidencia;
import com.benchify.incidencia.domain.TipoIncidencia;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;

import java.util.Map;
import java.util.UUID;

public interface IncidenciasUseCase {

    /** Avisa de un problema con un banco visible; cada usuario puede tener un aviso abierto por banco. */
    Incidencia reportar(Actor actor, UUID bancoId, TipoIncidencia tipo, String comentario);

    /** Avisos abiertos de un banco por tipo, para mostrarlos en su ficha. */
    Map<TipoIncidencia, Long> abiertasPorTipo(UUID bancoId, Actor actor);

    /** Cola de moderación: abiertas, de la más antigua a la más reciente. Solo administradores. */
    PageResult<Incidencia> abiertas(Actor actor, PageQuery page);

    Incidencia cerrar(Actor actor, UUID incidenciaId, EstadoIncidencia resultado);
}
