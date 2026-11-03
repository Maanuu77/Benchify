package com.benchify.notificacion.infrastructure.persistence;

import com.benchify.moderacion.application.port.out.EventoModeracionRepository;
import com.benchify.moderacion.domain.AccionModeracion;
import com.benchify.moderacion.domain.EventoModeracion;
import com.benchify.notificacion.application.port.out.NotificacionRepository;
import com.benchify.notificacion.domain.Notificacion;
import com.benchify.notificacion.domain.TipoNotificacion;
import com.benchify.shared.domain.PageQuery;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class NotificacionPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private NotificacionRepository notificaciones;
    @Autowired
    private EventoModeracionRepository eventos;
    @Autowired
    private TestData testData;

    @BeforeEach
    void usuarios() {
        testData.usuario("noti-ana");
        testData.usuario("noti-admin");
    }

    @Test
    void storesListsNewestFirstAndMarksAsRead() {
        Notificacion vieja = Notificacion.nueva("noti-ana", TipoNotificacion.FOTO_APROBADA, "Vieja", null, null, AHORA);
        Notificacion nueva = Notificacion.nueva("noti-ana", TipoNotificacion.BANCO_RECHAZADO, "Nueva", "Repetido",
                "/bancos/x", AHORA.plusSeconds(60));
        notificaciones.guardar(vieja);
        notificaciones.guardar(nueva);

        assertThat(notificaciones.findByUsuario("noti-ana", PageQuery.first(10)).content()).containsExactly(nueva, vieja);
        assertThat(notificaciones.contarNoLeidas("noti-ana")).isEqualTo(2);

        notificaciones.marcarLeida(vieja.id());
        assertThat(notificaciones.findById(vieja.id())).get().extracting(Notificacion::leida).isEqualTo(true);
        assertThat(notificaciones.contarNoLeidas("noti-ana")).isEqualTo(1);

        notificaciones.marcarTodasLeidas("noti-ana");
        assertThat(notificaciones.contarNoLeidas("noti-ana")).isZero();
    }

    @Test
    void keepsTheModerationHistoryNewestFirst() {
        EventoModeracion primero = EventoModeracion.registrar("noti-admin", AccionModeracion.BANCO_APROBADO,
                UUID.randomUUID(), null, AHORA.plusSeconds(3600));
        EventoModeracion segundo = EventoModeracion.registrar("noti-admin", AccionModeracion.IMAGEN_RECHAZADA,
                UUID.randomUUID(), "Borrosa", AHORA.plusSeconds(3660));
        eventos.guardar(primero);
        eventos.guardar(segundo);

        assertThat(eventos.findRecientes(PageQuery.first(2)).content()).containsExactly(segundo, primero);
    }
}
