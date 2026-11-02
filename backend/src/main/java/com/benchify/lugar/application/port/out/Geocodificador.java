package com.benchify.lugar.application.port.out;

import com.benchify.lugar.domain.Busqueda;
import com.benchify.lugar.domain.Lugar;

import java.util.List;

/** Servicio externo que traduce texto a coordenadas. */
public interface Geocodificador {

    /** @throws com.benchify.shared.domain.ServiceUnavailableException si no responde o está saturado */
    List<Lugar> buscar(Busqueda busqueda, int maximo);
}
