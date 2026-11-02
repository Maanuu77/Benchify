package com.benchify.lugar.application.service;

import com.benchify.lugar.application.port.in.BuscarLugaresUseCase;
import com.benchify.lugar.application.port.out.Geocodificador;
import com.benchify.lugar.domain.Busqueda;
import com.benchify.lugar.domain.Lugar;
import com.benchify.shared.domain.Ubicacion;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class LugarService implements BuscarLugaresUseCase {

    static final int MAX_RESULTADOS = 5;

    private final Geocodificador geocodificador;

    LugarService(Geocodificador geocodificador) {
        this.geocodificador = geocodificador;
    }

    @Override
    public List<Lugar> buscar(String texto, String idioma, Ubicacion cerca) {
        return geocodificador.buscar(Busqueda.de(texto, idioma, cerca), MAX_RESULTADOS);
    }
}
