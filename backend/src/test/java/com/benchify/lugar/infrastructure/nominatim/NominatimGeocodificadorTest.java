package com.benchify.lugar.infrastructure.nominatim;

import com.benchify.lugar.domain.Busqueda;
import com.benchify.lugar.domain.Encuadre;
import com.benchify.lugar.domain.Lugar;
import com.benchify.shared.domain.ServiceUnavailableException;
import com.benchify.shared.domain.Ubicacion;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NominatimGeocodificadorTest {

    private static final String RESPUESTA = """
            [
              {"lat": "37.3772", "lon": "-5.9869", "name": "Plaza de España",
               "display_name": "Plaza de España, Sevilla, Andalucía, España",
               "boundingbox": ["37.3755", "37.3790", "-5.9890", "-5.9848"]},
              {"lat": "37.38", "lon": "-5.99", "name": "",
               "display_name": "Calle Plaza de España, Dos Hermanas, España"},
              {"lat": "no es un número", "lon": "-5.99", "name": "Roto"}
            ]
            """;

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();

    private NominatimGeocodificador geocodificador(Duration intervalo, Duration esperaMaxima) {
        return new NominatimGeocodificador(builder, new GeocodificacionProperties("https://nominatim.test",
                "Benchify-test", Duration.ofSeconds(1), intervalo, esperaMaxima, Duration.ofHours(1)));
    }

    @Test
    void searchesNearTheMapCentreAndMapsTheResults() {
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("https://nominatim.test/search?")))
                .andExpect(header(HttpHeaders.USER_AGENT, "Benchify-test"))
                .andExpect(queryParam("q", "Plaza%20de%20Espa%C3%B1a"))
                .andExpect(queryParam("format", "jsonv2"))
                .andExpect(queryParam("limit", "5"))
                .andExpect(queryParam("accept-language", "en"))
                .andExpect(queryParam("viewbox", "-6.3,37.7,-5.7,37.1"))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        List<Lugar> lugares = geocodificador(Duration.ofMillis(1), Duration.ofSeconds(1))
                .buscar(Busqueda.de("Plaza de España", "en", new Ubicacion(37.4, -6.0)), 5);

        servidor.verify();
        assertThat(lugares).containsExactly(
                new Lugar("Plaza de España", "Plaza de España, Sevilla, Andalucía, España", new Ubicacion(37.3772, -5.9869),
                        new Encuadre(37.3755, -5.9890, 37.3790, -5.9848)),
                new Lugar("Calle Plaza de España", "Calle Plaza de España, Dos Hermanas, España",
                        new Ubicacion(37.38, -5.99), null));
    }

    @Test
    void repeatedSearchesComeFromTheCache() {
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("https://nominatim.test/search?")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        NominatimGeocodificador geocodificador = geocodificador(Duration.ofHours(1), Duration.ofMillis(10));

        assertThat(geocodificador.buscar(Busqueda.de("Triana", "es", null), 5)).isEmpty();
        assertThat(geocodificador.buscar(Busqueda.de("  triana ", "es", null), 5)).isEmpty();

        servidor.verify();
    }

    @Test
    void sharesOneRequestPerIntervalAmongAllUsers() {
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("https://nominatim.test/search?")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        NominatimGeocodificador geocodificador = geocodificador(Duration.ofHours(1), Duration.ofMillis(10));
        geocodificador.buscar(Busqueda.de("Triana", "es", null), 5);

        assertThatThrownBy(() -> geocodificador.buscar(Busqueda.de("Macarena", "es", null), 5))
                .isInstanceOf(ServiceUnavailableException.class)
                .extracting("code").isEqualTo(NominatimGeocodificador.CODE);
    }

    @Test
    void failuresAreReportedAsUnavailableAndNotCached() {
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("https://nominatim.test/search?")))
                .andRespond(withServerError());
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith("https://nominatim.test/search?")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        NominatimGeocodificador geocodificador = geocodificador(Duration.ofMillis(1), Duration.ofSeconds(1));

        assertThatThrownBy(() -> geocodificador.buscar(Busqueda.de("Triana", "es", null), 5))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThat(geocodificador.buscar(Busqueda.de("Triana", "es", null), 5)).isEmpty();
        servidor.verify();
    }
}
