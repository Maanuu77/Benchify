package com.benchify.lugar.application.port.in;

import com.benchify.lugar.domain.Lugar;
import com.benchify.shared.domain.Ubicacion;

import java.util.List;

public interface BuscarLugaresUseCase {

    /**
     * Direcciones o sitios que encajan con el texto, los más cercanos a {@code cerca} primero.
     *
     * @param idioma código del idioma preferido ("es" o "en")
     * @param cerca  centro del mapa; puede ser null
     */
    List<Lugar> buscar(String texto, String idioma, Ubicacion cerca);
}
