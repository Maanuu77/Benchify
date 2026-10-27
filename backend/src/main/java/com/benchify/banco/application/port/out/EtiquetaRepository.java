package com.benchify.banco.application.port.out;

import com.benchify.banco.domain.Etiqueta;

import java.util.Collection;
import java.util.List;

public interface EtiquetaRepository {

    /** Catálogo completo ordenado por nombre. */
    List<Etiqueta> findAll();

    List<Etiqueta> findAllById(Collection<Long> ids);
}
