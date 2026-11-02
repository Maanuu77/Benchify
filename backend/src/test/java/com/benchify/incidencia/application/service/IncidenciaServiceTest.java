package com.benchify.incidencia.application.service;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.incidencia.application.port.out.IncidenciaRepository;
import com.benchify.incidencia.domain.EstadoIncidencia;
import com.benchify.incidencia.domain.Incidencia;
import com.benchify.incidencia.domain.TipoIncidencia;
import com.benchify.notificacion.application.port.in.NotificacionesUseCase;
import com.benchify.notificacion.application.port.in.NotificacionesUseCase.NuevaNotificacion;
import com.benchify.notificacion.domain.TipoNotificacion;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ConflictException;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.Rol;
import com.benchify.shared.domain.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T10:00:00Z");
    private static final Actor ANA = Actor.user("ana");
    private static final Actor ADMIN = Actor.admin("root");

    @Mock
    private IncidenciaRepository incidencias;
    @Mock
    private BancoRepository bancos;
    @Mock
    private NotificacionesUseCase notificaciones;

    private IncidenciaService service;
    private Banco banco;

    @BeforeEach
    void setUp() {
        service = new IncidenciaService(incidencias, bancos, notificaciones, Clock.fixed(AHORA, ZoneOffset.UTC));
        banco = Banco.proponer(UUID.randomUUID(), new DatosBanco("Glorieta", null, new Ubicacion(37, -6), null, null, null),
                ADMIN, AHORA);
    }

    @Test
    void reportsAProblemOnAVisibleBench() {
        when(bancos.findById(banco.id())).thenReturn(Optional.of(banco));

        Incidencia incidencia = service.reportar(ANA, banco.id(), TipoIncidencia.ROTO, "  Le falta una tabla  ");

        assertThat(incidencia.estado()).isEqualTo(EstadoIncidencia.ABIERTA);
        assertThat(incidencia.comentario()).isEqualTo("Le falta una tabla");
        verify(incidencias).guardar(incidencia);
    }

    @Test
    void oneOpenReportPerUserAndBench() {
        when(bancos.findById(banco.id())).thenReturn(Optional.of(banco));
        when(incidencias.existeAbierta(banco.id(), "ana")).thenReturn(true);

        assertThatThrownBy(() -> service.reportar(ANA, banco.id(), TipoIncidencia.SUCIO, null))
                .isInstanceOf(ConflictException.class);
        verify(incidencias, never()).guardar(any());
    }

    @Test
    void reportingRequiresAVerifiedEmailAndAVisibleBench() {
        assertThatThrownBy(() -> service.reportar(new Actor("ana", Rol.USER, false), banco.id(), TipoIncidencia.ROTO, null))
                .isInstanceOf(ForbiddenException.class);
        when(bancos.findById(banco.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reportar(ANA, banco.id(), TipoIncidencia.ROTO, null))
                .isInstanceOf(BancoNotFoundException.class);
        assertThatThrownBy(() -> Incidencia.reportar(banco.id(), ANA, TipoIncidencia.OTRO, " ", AHORA))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void closingNotifiesTheReporter() {
        Incidencia abierta = Incidencia.reportar(banco.id(), ANA, TipoIncidencia.DESAPARECIDO, null, AHORA);
        when(incidencias.findById(abierta.id())).thenReturn(Optional.of(abierta));
        when(bancos.findById(banco.id())).thenReturn(Optional.of(banco));

        Incidencia cerrada = service.cerrar(ADMIN, abierta.id(), EstadoIncidencia.RESUELTA);

        assertThat(cerrada.estado()).isEqualTo(EstadoIncidencia.RESUELTA);
        assertThat(cerrada.resueltaPor()).isEqualTo("root");
        verify(incidencias).actualizar(cerrada);
        ArgumentCaptor<NuevaNotificacion> aviso = ArgumentCaptor.forClass(NuevaNotificacion.class);
        verify(notificaciones).notificar(eq("ana"), aviso.capture());
        assertThat(aviso.getValue().tipo()).isEqualTo(TipoNotificacion.INCIDENCIA_REVISADA);
        assertThat(aviso.getValue().titulo()).isEqualTo("Hemos revisado tu aviso sobre «Glorieta»");
        assertThatThrownBy(() -> cerrada.cerrar(ADMIN, EstadoIncidencia.DESCARTADA, AHORA))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void onlyAdminsReviewIncidents() {
        assertThatThrownBy(() -> service.cerrar(ANA, UUID.randomUUID(), EstadoIncidencia.RESUELTA))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.abiertas(ANA, null)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(incidencias, notificaciones);
    }
}
