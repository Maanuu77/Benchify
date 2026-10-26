package com.benchify.checkin.domain;

import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ConflictException;
import com.benchify.shared.domain.Ubicacion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CheckInTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");
    private static final PoliticaCheckIn POLITICA = new PoliticaCheckIn(150, Duration.ofHours(12));
    private static final Ubicacion BANCO = new Ubicacion(37.38250, -5.99600);
    private static final UUID BANCO_ID = UUID.randomUUID();

    @Test
    void registersWhenTheUserIsCloseEnough() {
        Ubicacion cerca = new Ubicacion(37.38300, -5.99600);

        CheckIn checkIn = CheckIn.registrar(UUID.randomUUID(), "ana", BANCO_ID, BANCO, cerca, Optional.empty(),
                POLITICA, AHORA);

        assertThat(checkIn.distanciaMetros()).isCloseTo(55.6, within(1.0));
        assertThat(checkIn.posicion()).isEqualTo(cerca);
        assertThat(checkIn.fecha()).isEqualTo(AHORA);
    }

    @Test
    void rejectsWhenTheUserIsTooFar() {
        Ubicacion lejos = new Ubicacion(37.39000, -5.99600);

        assertThatThrownBy(() -> CheckIn.registrar(UUID.randomUUID(), "ana", BANCO_ID, BANCO, lejos, Optional.empty(),
                POLITICA, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("834 m")
                .extracting("code").isEqualTo("CHECKIN_DEMASIADO_LEJOS");
    }

    @Test
    void rejectsARepeatedCheckInInsideTheInterval() {
        assertThatThrownBy(() -> CheckIn.registrar(UUID.randomUUID(), "ana", BANCO_ID, BANCO, BANCO,
                Optional.of(AHORA.minus(Duration.ofHours(11))), POLITICA, AHORA))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("CHECKIN_REPETIDO");
    }

    @Test
    void allowsANewCheckInOnceTheIntervalHasPassed() {
        CheckIn checkIn = CheckIn.registrar(UUID.randomUUID(), "ana", BANCO_ID, BANCO, BANCO,
                Optional.of(AHORA.minus(Duration.ofHours(12))), POLITICA, AHORA);

        assertThat(checkIn.distanciaMetros()).isZero();
    }

    @Test
    void policyRequiresAPositiveDistance() {
        assertThatThrownBy(() -> new PoliticaCheckIn(0, Duration.ofHours(1))).isInstanceOf(IllegalArgumentException.class);
    }
}
