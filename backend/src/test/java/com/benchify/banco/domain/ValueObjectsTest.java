package com.benchify.banco.domain;

import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.Ubicacion;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ValueObjectsTest {

    @Test
    void ubicacionRejectsOutOfRangeCoordinates() {
        assertThatThrownBy(() -> new Ubicacion(91, 0)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Ubicacion(0, -181)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Ubicacion(Double.NaN, 0)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void haversineDistanceBetweenTwoSevilleLandmarks() {
        Ubicacion torreDelOro = new Ubicacion(37.38232, -5.99632);
        Ubicacion giralda = new Ubicacion(37.38614, -5.99260);

        assertThat(torreDelOro.distanciaMetrosA(giralda)).isCloseTo(535, within(15.0));
        assertThat(torreDelOro.distanciaMetrosA(torreDelOro)).isZero();
    }

    @Test
    void boundingBoxRequiresMinBelowMax() {
        assertThat(new BoundingBox(37.3, -6.1, 37.4, -5.9).maxLat()).isEqualTo(37.4);
        assertThatThrownBy(() -> new BoundingBox(37.4, -6.1, 37.3, -5.9))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("BOUNDING_BOX_INVALIDA");
    }

    @Test
    void datosBancoTrimsAndAppliesDefaults() {
        DatosBanco datos = new DatosBanco("  Banco  ", "   ", new Ubicacion(0, 0), null, null, null);

        assertThat(datos.titulo()).isEqualTo("Banco");
        assertThat(datos.descripcion()).isNull();
        assertThat(datos.direccion().pais()).isEqualTo("España");
        assertThat(datos.caracteristicas().accesible()).isFalse();
        assertThat(datos.etiquetas()).isEmpty();
    }

    @Test
    void datosBancoValidatesLengthsAndTagCount() {
        Ubicacion u = new Ubicacion(0, 0);
        assertThatThrownBy(() -> new DatosBanco(" ", null, u, null, null, null))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("BANCO_TITULO_INVALIDO");
        assertThatThrownBy(() -> new DatosBanco("x".repeat(151), null, u, null, null, null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new DatosBanco("ok", "x".repeat(501), u, null, null, null))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("BANCO_DESCRIPCION_INVALIDA");

        Set<Etiqueta> seis = LongStream.rangeClosed(1, 6)
                .mapToObj(i -> new Etiqueta(i, "e" + i, "#000000", "tag"))
                .collect(Collectors.toSet());
        assertThatThrownBy(() -> new DatosBanco("ok", null, u, null, null, seis))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("BANCO_DEMASIADAS_ETIQUETAS");
    }

    @Test
    void direccionDefaultsCountryAndClearsBlanks() {
        DireccionPostal direccion = new DireccionPostal(" ", " Sevilla ", null, " Portugal ");

        assertThat(direccion.direccion()).isNull();
        assertThat(direccion.ciudad()).isEqualTo("Sevilla");
        assertThat(direccion.pais()).isEqualTo("Portugal");
    }

    @Test
    void filtroRejectsInvalidMinimumRating() {
        assertThatThrownBy(() -> new FiltroBancos(null, null, null, null, 6.0)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void valoracionRejectsImpossibleValues() {
        assertThatThrownBy(() -> new Valoracion(5.5, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Valoracion(3, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
