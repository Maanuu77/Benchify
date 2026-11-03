package com.benchify.lugar.domain;

import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.Ubicacion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusquedaTest {

    @Test
    void cleansTheTextAndPicksALanguage() {
        Busqueda busqueda = Busqueda.de("  Plaza   de\tEspaña ", "en-GB,en;q=0.9", null);

        assertThat(busqueda.texto()).isEqualTo("Plaza de España");
        assertThat(busqueda.idioma()).isEqualTo("en");
        assertThat(Busqueda.de("Triana", null, null).idioma()).isEqualTo("es");
        assertThat(Busqueda.de("Triana", "fr", null).idioma()).isEqualTo("es");
    }

    @Test
    void rejectsTooShortOrTooLongTexts() {
        assertThatThrownBy(() -> Busqueda.de(" ab ", "es", null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> Busqueda.de(null, "es", null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> Busqueda.de("x".repeat(121), "es", null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void nearbyCentresShareTheCacheKey() {
        Busqueda aqui = Busqueda.de("Alameda", "es", new Ubicacion(37.3891, -5.9845));
        Busqueda alLado = Busqueda.de("alameda", "es", new Ubicacion(37.4012, -5.9711));
        Busqueda lejos = Busqueda.de("Alameda", "es", new Ubicacion(40.4168, -3.7038));

        assertThat(aqui.clave()).isEqualTo(alLado.clave());
        assertThat(aqui.clave()).isNotEqualTo(lejos.clave());
        assertThat(aqui.clave()).isNotEqualTo(Busqueda.de("Alameda", "en", new Ubicacion(37.3891, -5.9845)).clave());
    }
}
