package com.benchify.notificacion.infrastructure.web;

import com.benchify.notificacion.application.port.in.NotificacionesUseCase;
import com.benchify.notificacion.domain.Notificacion;
import com.benchify.notificacion.domain.TipoNotificacion;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.support.ApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiControllerTest(NotificacionController.class)
class NotificacionControllerTest {

    private static final Actor ANA = Actor.user("ana");

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private NotificacionesUseCase notificaciones;

    @Test
    void listsAndCountsOwnNotifications() {
        Notificacion aviso = Notificacion.nueva("ana", TipoNotificacion.BANCO_APROBADO, "Tu banco ya está publicado",
                null, "/bancos/1", Instant.parse("2026-11-01T10:00:00Z"));
        when(notificaciones.mias(ANA, new PageQuery(0, 20))).thenReturn(new PageResult<>(List.of(aviso), 0, 20, 1));
        when(notificaciones.noLeidas(ANA)).thenReturn(1L);

        assertThat(mvc.get().uri("/api/v1/usuarios/me/notificaciones").header("Authorization", bearer("ana")))
                .hasStatusOk()
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].tipo").isEqualTo("BANCO_APROBADO");
                    assertThat(json).extractingPath("$.content[0].enlace").isEqualTo("/bancos/1");
                });
        assertThat(mvc.get().uri("/api/v1/usuarios/me/notificaciones/no-leidas").header("Authorization", bearer("ana")))
                .hasStatusOk().bodyJson().extractingPath("$.total").isEqualTo(1);
    }

    @Test
    void marksAsRead() {
        Notificacion aviso = Notificacion.nueva("ana", TipoNotificacion.FOTO_APROBADA, "Foto", null, null, Instant.now());

        assertThat(mvc.post().uri("/api/v1/notificaciones/{id}/leida", aviso.id()).header("Authorization", bearer("ana")))
                .hasStatus(204);
        assertThat(mvc.post().uri("/api/v1/usuarios/me/notificaciones/leidas").header("Authorization", bearer("ana")))
                .hasStatus(204);
        verify(notificaciones).marcarLeida(ANA, aviso.id());
        verify(notificaciones).marcarTodasLeidas(ANA);
    }

    @Test
    void requiresAuthentication() {
        assertThat(mvc.get().uri("/api/v1/usuarios/me/notificaciones")).hasStatus(401);
    }
}
