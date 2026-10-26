package com.benchify.resena.application.service;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.Ubicacion;
import com.benchify.banco.domain.Valoracion;
import com.benchify.resena.application.port.in.ResenasUseCase.ResenaCommand;
import com.benchify.resena.application.port.out.ResenaRepository;
import com.benchify.resena.domain.Resena;
import com.benchify.resena.domain.ResenaNotFoundException;
import com.benchify.resena.domain.ResumenPuntuacion;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ConflictException;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");
    private static final Actor AUTOR_BANCO = Actor.user("autor");
    private static final Actor ANA = Actor.user("ana");
    private static final Actor LUIS = Actor.user("luis");

    @Mock
    private ResenaRepository resenas;
    @Mock
    private BancoRepository bancos;

    private ResenaService service;
    private Banco banco;

    @BeforeEach
    void setUp() {
        service = new ResenaService(resenas, bancos, Clock.fixed(AHORA, ZoneOffset.UTC));
        banco = Banco.proponer(UUID.randomUUID(), new DatosBanco("Banco", null, new Ubicacion(37, -6), null, null, null),
                AUTOR_BANCO, AHORA);
        banco.aprobar(AHORA);
        lenient().when(bancos.findByIdForUpdate(banco.id())).thenReturn(Optional.of(banco));
        lenient().when(bancos.findById(banco.id())).thenReturn(Optional.of(banco));
        lenient().when(resenas.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void publishingRecalculatesTheBenchRating() {
        when(resenas.resumen(banco.id())).thenReturn(new ResumenPuntuacion(4.666, 3));

        Resena resena = service.publicar(ANA, banco.id(), new ResenaCommand(5, "Genial"));

        assertThat(resena.autorId()).isEqualTo("ana");
        assertThat(resena.fecha()).isEqualTo(AHORA);
        assertThat(banco.valoracion()).isEqualTo(new Valoracion(4.7, 3));
        verify(bancos).save(banco);
    }

    @Test
    void cannotReviewUnpublishedHiddenOrOwnBenches() {
        Banco pendiente = Banco.proponer(UUID.randomUUID(),
                new DatosBanco("Pendiente", null, new Ubicacion(37, -6), null, null, null), ANA, AHORA);
        when(bancos.findByIdForUpdate(pendiente.id())).thenReturn(Optional.of(pendiente));

        assertThatThrownBy(() -> service.publicar(LUIS, pendiente.id(), new ResenaCommand(3, null)))
                .isInstanceOf(BancoNotFoundException.class);
        assertThatThrownBy(() -> service.publicar(ANA, pendiente.id(), new ResenaCommand(3, null)))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("BANCO_NO_PUBLICADO");
        assertThatThrownBy(() -> service.publicar(AUTOR_BANCO, banco.id(), new ResenaCommand(5, null)))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("RESENA_BANCO_PROPIO");
        verify(resenas, never()).save(any());
    }

    @Test
    void secondReviewOfTheSameBenchIsAConflict() {
        when(resenas.existsByBancoAndAutor(banco.id(), "ana")).thenReturn(true);

        assertThatThrownBy(() -> service.publicar(ANA, banco.id(), new ResenaCommand(4, null)))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("RESENA_DUPLICADA");
    }

    @Test
    void editingAndDeletingRecalculateTheRating() {
        Resena resena = Resena.publicar(UUID.randomUUID(), banco.id(), ANA, 5, null, AHORA);
        when(resenas.findById(resena.id())).thenReturn(Optional.of(resena));
        when(resenas.resumen(banco.id())).thenReturn(new ResumenPuntuacion(2, 1), ResumenPuntuacion.VACIO);

        Resena editada = service.editar(ANA, resena.id(), new ResenaCommand(2, "Peor de lo que pensaba"));
        assertThat(editada.puntuacion()).isEqualTo(2);
        assertThat(banco.valoracion()).isEqualTo(new Valoracion(2, 1));

        service.eliminar(ANA, resena.id());
        verify(resenas).delete(resena.id());
        assertThat(banco.valoracion()).isEqualTo(Valoracion.SIN_RESENAS);
    }

    @Test
    void othersCannotDeleteAReview() {
        Resena resena = Resena.publicar(UUID.randomUUID(), banco.id(), ANA, 5, null, AHORA);
        when(resenas.findById(resena.id())).thenReturn(Optional.of(resena));

        assertThatThrownBy(() -> service.eliminar(LUIS, resena.id())).isInstanceOf(ForbiddenException.class);
        verify(resenas, never()).delete(any());
    }

    @Test
    void reportingCountsEachUserOnce() {
        Resena resena = Resena.publicar(UUID.randomUUID(), banco.id(), ANA, 1, "Spam", AHORA);
        when(resenas.findByIdForUpdate(resena.id())).thenReturn(Optional.of(resena));
        when(resenas.registrarReporte(resena.id(), "luis", AHORA)).thenReturn(true, false);

        assertThat(service.reportar(LUIS, resena.id()).numReportes()).isEqualTo(1);
        assertThatThrownBy(() -> service.reportar(LUIS, resena.id()))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("RESENA_YA_REPORTADA");
    }

    @Test
    void unknownReviewIs404() {
        UUID id = UUID.randomUUID();
        when(resenas.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportar(LUIS, id)).isInstanceOf(ResenaNotFoundException.class);
    }

    @Test
    void onlyAdminsDismissReports() {
        Resena reportada = Resena.publicar(UUID.randomUUID(), banco.id(), ANA, 1, null, AHORA)
                .conReporteAdicional().conReporteAdicional().conReporteAdicional();
        lenient().when(resenas.findById(reportada.id())).thenReturn(Optional.of(reportada));

        assertThatThrownBy(() -> service.descartarReportes(LUIS, reportada.id())).isInstanceOf(ForbiddenException.class);

        Resena revisada = service.descartarReportes(Actor.admin("root"), reportada.id());
        assertThat(revisada.reportada()).isFalse();
        verify(resenas).borrarReportes(reportada.id());
    }

    @Test
    void listingABenchRequiresItToBeVisible() {
        UUID oculto = UUID.randomUUID();
        when(bancos.findById(oculto)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deBanco(oculto, null, PageQuery.first(10)))
                .isInstanceOf(BancoNotFoundException.class);

        when(resenas.findByBanco(banco.id(), PageQuery.first(10))).thenReturn(PageResult.empty(PageQuery.first(10)));
        assertThat(service.deBanco(banco.id(), null, PageQuery.first(10)).content()).isEmpty();
    }
}
