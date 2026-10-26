package com.benchify.resena.domain;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResenaTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");
    private static final Actor ANA = Actor.user("ana");

    private static Resena resena() {
        return Resena.publicar(UUID.randomUUID(), UUID.randomUUID(), ANA, 4, "  Muy cómodo  ", AHORA);
    }

    @Test
    void publishesWithTrimmedComment() {
        Resena resena = resena();

        assertThat(resena.comentario()).isEqualTo("Muy cómodo");
        assertThat(resena.autorId()).isEqualTo("ana");
        assertThat(resena.reportada()).isFalse();
        assertThat(resena.fechaEdicion()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    void rejectsScoresOutsideOneToFive(int puntuacion) {
        assertThatThrownBy(() -> Resena.publicar(UUID.randomUUID(), UUID.randomUUID(), ANA, puntuacion, null, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("RESENA_PUNTUACION_INVALIDA");
    }

    @Test
    void rejectsTooLongCommentsAndClearsBlankOnes() {
        assertThatThrownBy(() -> Resena.publicar(UUID.randomUUID(), UUID.randomUUID(), ANA, 3, "x".repeat(1001), AHORA))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(Resena.publicar(UUID.randomUUID(), UUID.randomUUID(), ANA, 3, "   ", AHORA).comentario()).isNull();
    }

    @Test
    void onlyTheAuthorEdits() {
        Resena editada = resena().editar(ANA, 2, "Ya no tanto", AHORA.plusSeconds(60));

        assertThat(editada.puntuacion()).isEqualTo(2);
        assertThat(editada.fechaEdicion()).isEqualTo(AHORA.plusSeconds(60));
        assertThat(editada.fecha()).isEqualTo(AHORA);
        assertThatThrownBy(() -> resena().editar(Actor.admin("root"), 1, null, AHORA))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void authorOrAdminDeletes() {
        assertThatCode(() -> resena().exigirBorradoPor(ANA)).doesNotThrowAnyException();
        assertThatCode(() -> resena().exigirBorradoPor(Actor.admin("root"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> resena().exigirBorradoPor(Actor.user("luis"))).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void thirdReportSendsTheReviewToModeration() {
        Resena resena = resena().conReporteAdicional().conReporteAdicional();
        assertThat(resena.reportada()).isFalse();

        Resena reportada = resena.conReporteAdicional();
        assertThat(reportada.numReportes()).isEqualTo(3);
        assertThat(reportada.reportada()).isTrue();

        Resena revisada = reportada.sinReportes();
        assertThat(revisada.numReportes()).isZero();
        assertThat(revisada.reportada()).isFalse();
    }

    @Test
    void authorsCannotReportTheirOwnReview() {
        assertThatThrownBy(() -> resena().exigirReportablePor(ANA))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("RESENA_PROPIA");
        assertThatCode(() -> resena().exigirReportablePor(Actor.user("luis"))).doesNotThrowAnyException();
    }
}
