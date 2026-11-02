package com.benchify.banco.domain;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.Ubicacion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RevisionBancoTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T10:00:00Z");
    private static final Actor ANA = Actor.user("ana");
    private static final Actor ADMIN = Actor.admin("root");

    @Test
    void aNewEditWaitsForModerationAndCanBeReplacedMeanwhile() {
        RevisionBanco revision = RevisionBanco.proponer(UUID.randomUUID(), ANA, datos("Primera"), AHORA);

        assertThat(revision.estado()).isEqualTo(EstadoRevision.PENDIENTE);
        assertThat(revision.autorId()).isEqualTo("ana");
        RevisionBanco sustituida = revision.sustituir(datos("Segunda"), AHORA.plusSeconds(60));
        assertThat(sustituida.id()).isEqualTo(revision.id());
        assertThat(sustituida.datos().titulo()).isEqualTo("Segunda");
        assertThat(sustituida.fecha()).isEqualTo(AHORA.plusSeconds(60));
    }

    @Test
    void isReviewedOnlyOnce() {
        RevisionBanco revision = RevisionBanco.proponer(UUID.randomUUID(), ANA, datos("Cambio"), AHORA);

        RevisionBanco aprobada = revision.aprobar(ADMIN, AHORA.plusSeconds(10));
        assertThat(aprobada.estado()).isEqualTo(EstadoRevision.APROBADA);
        assertThat(aprobada.revisadaPor()).isEqualTo("root");
        assertThat(aprobada.fechaRevision()).isEqualTo(AHORA.plusSeconds(10));
        assertThat(revision.rechazar(ADMIN, AHORA).estado()).isEqualTo(EstadoRevision.RECHAZADA);

        assertThatThrownBy(() -> aprobada.rechazar(ADMIN, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("REVISION_NO_PENDIENTE");
        assertThatThrownBy(() -> aprobada.sustituir(datos("Tarde"), AHORA)).isInstanceOf(BusinessRuleException.class);
    }

    private static DatosBanco datos(String titulo) {
        return new DatosBanco(titulo, null, new Ubicacion(37.38, -6.0), null, null, null);
    }
}
