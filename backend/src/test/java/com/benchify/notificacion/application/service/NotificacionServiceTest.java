package com.benchify.notificacion.application.service;

import com.benchify.notificacion.application.port.in.NotificacionesUseCase.NuevaNotificacion;
import com.benchify.notificacion.application.port.out.NotificacionRepository;
import com.benchify.notificacion.domain.Notificacion;
import com.benchify.notificacion.domain.TipoNotificacion;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.NotFoundException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-01T10:00:00Z");

    @Mock
    private NotificacionRepository repository;

    private NotificacionService service;

    @BeforeEach
    void setUp() {
        service = new NotificacionService(repository, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void storesANewUnreadNotification() {
        service.notificar("ana", new NuevaNotificacion(TipoNotificacion.BANCO_APROBADO, "Publicado", "  ", "/bancos/1"));

        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository).guardar(guardada.capture());
        assertThat(guardada.getValue().usuarioId()).isEqualTo("ana");
        assertThat(guardada.getValue().leida()).isFalse();
        assertThat(guardada.getValue().mensaje()).isNull();
        assertThat(guardada.getValue().fecha()).isEqualTo(AHORA);
    }

    @Test
    void onlyTheRecipientCanMarkItAsRead() {
        Notificacion deAna = Notificacion.nueva("ana", TipoNotificacion.FOTO_APROBADA, "Foto", null, null, AHORA);
        when(repository.findById(deAna.id())).thenReturn(Optional.of(deAna));

        assertThatThrownBy(() -> service.marcarLeida(Actor.user("luis"), deAna.id())).isInstanceOf(NotFoundException.class);
        verify(repository, never()).marcarLeida(any());

        service.marcarLeida(Actor.user("ana"), deAna.id());
        verify(repository).marcarLeida(deAna.id());
    }

    @Test
    void longTextsAreShortened() {
        Notificacion larga = Notificacion.nueva("ana", TipoNotificacion.BANCO_RECHAZADO, "t".repeat(300), "m".repeat(600),
                null, AHORA);

        assertThat(larga.titulo()).hasSize(Notificacion.MAX_TITULO).endsWith("…");
        assertThat(larga.mensaje()).hasSize(Notificacion.MAX_MENSAJE);
    }
}
