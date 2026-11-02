package com.benchify.lugar.infrastructure.web;

import com.benchify.lugar.application.port.in.BuscarLugaresUseCase;
import com.benchify.lugar.domain.Encuadre;
import com.benchify.lugar.domain.Lugar;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ServiceUnavailableException;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.support.ApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ApiControllerTest(LugarController.class)
class LugarControllerTest {

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private BuscarLugaresUseCase lugares;

    @Test
    void searchesAnonymouslyNearTheMapCentre() {
        when(lugares.buscar("Plaza de España", "en", new Ubicacion(37.4, -6.0))).thenReturn(List.of(
                new Lugar("Plaza de España", "Plaza de España, Sevilla", new Ubicacion(37.3772, -5.9869),
                        new Encuadre(37.3755, -5.989, 37.379, -5.9848)),
                new Lugar("Calle", "Calle, Dos Hermanas", new Ubicacion(37.28, -5.92), null)));

        assertThat(mvc.get().uri("/api/v1/lugares").param("q", "Plaza de España").param("lat", "37.4").param("lng", "-6.0")
                .header("Accept-Language", "en"))
                .hasStatusOk()
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$[0].nombre").isEqualTo("Plaza de España");
                    assertThat(json).extractingPath("$[0].norte").isEqualTo(37.379);
                    assertThat(json).extractingPath("$[1].sur").isNull();
                });
    }

    @Test
    void mapsInvalidSearchesAndUnavailableServices() {
        when(lugares.buscar(anyString(), isNull(), isNull()))
                .thenThrow(new BusinessRuleException("BUSQUEDA_INVALIDA", "Escribe entre 3 y 120 caracteres"));
        assertThat(mvc.get().uri("/api/v1/lugares").param("q", "ab"))
                .hasStatus(422)
                .bodyJson().extractingPath("$.code").isEqualTo("BUSQUEDA_INVALIDA");

        doThrow(new ServiceUnavailableException("GEOCODIFICACION_NO_DISPONIBLE", "No responde"))
                .when(lugares).buscar(anyString(), any(), any());
        assertThat(mvc.get().uri("/api/v1/lugares").param("q", "Triana"))
                .hasStatus(503)
                .hasHeader("Retry-After", "5")
                .bodyJson().extractingPath("$.code").isEqualTo("GEOCODIFICACION_NO_DISPONIBLE");
    }
}
