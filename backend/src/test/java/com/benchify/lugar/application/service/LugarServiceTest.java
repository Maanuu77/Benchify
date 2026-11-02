package com.benchify.lugar.application.service;

import com.benchify.lugar.application.port.out.Geocodificador;
import com.benchify.lugar.domain.Busqueda;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.Ubicacion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LugarServiceTest {

    private final Geocodificador geocodificador = mock(Geocodificador.class);
    private final LugarService service = new LugarService(geocodificador);

    @Test
    void asksForAFewCleanedResults() {
        Busqueda esperada = Busqueda.de("Triana", "es", new Ubicacion(37.38, -6));
        when(geocodificador.buscar(esperada, LugarService.MAX_RESULTADOS)).thenReturn(List.of());

        assertThat(service.buscar(" Triana ", "es-ES", new Ubicacion(37.38, -6))).isEmpty();
        verify(geocodificador).buscar(esperada, LugarService.MAX_RESULTADOS);
    }

    @Test
    void invalidSearchesNeverReachTheService() {
        assertThatThrownBy(() -> service.buscar("a", "es", null)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(geocodificador);
    }
}
