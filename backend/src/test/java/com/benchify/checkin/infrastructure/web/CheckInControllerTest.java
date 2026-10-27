package com.benchify.checkin.infrastructure.web;

import com.benchify.checkin.application.port.in.CheckInConBanco;
import com.benchify.checkin.application.port.in.CheckInsUseCase;
import com.benchify.checkin.domain.CheckIn;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.support.ApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiControllerTest(CheckInController.class)
@Import(CheckInWebMapperImpl.class)
class CheckInControllerTest {

    private static final UUID BANCO = UUID.randomUUID();
    private static final Ubicacion POSICION = new Ubicacion(37.3826, -5.996);
    private static final CheckIn CHECK_IN = new CheckIn(UUID.randomUUID(), "ana", BANCO, POSICION, 11.4,
            Instant.parse("2026-10-25T10:00:00Z"));

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private CheckInsUseCase checkIns;

    @Test
    void registersCheckIn() {
        when(checkIns.registrar(Actor.user("ana"), BANCO, POSICION)).thenReturn(CHECK_IN);

        assertThat(mvc.post().uri("/api/v1/bancos/{id}/checkins", BANCO).header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":37.3826,\"lng\":-5.996}"))
                .hasStatus(201)
                .bodyJson().extractingPath("$.distanciaMetros").isEqualTo(11);
    }

    @Test
    void tooFarIsUnprocessable() {
        when(checkIns.registrar(Actor.user("ana"), BANCO, POSICION))
                .thenThrow(new BusinessRuleException("CHECKIN_DEMASIADO_LEJOS", "Estás a 834 m"));

        assertThat(mvc.post().uri("/api/v1/bancos/{id}/checkins", BANCO).header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":37.3826,\"lng\":-5.996}"))
                .hasStatus(422)
                .bodyJson().extractingPath("$.code").isEqualTo("CHECKIN_DEMASIADO_LEJOS");
    }

    @Test
    void requiresPositionAndAuthentication() {
        assertThat(mvc.post().uri("/api/v1/bancos/{id}/checkins", BANCO).header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":37.3826}")).hasStatus(400);
        assertThat(mvc.post().uri("/api/v1/bancos/{id}/checkins", BANCO)
                .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":1,\"lng\":1}")).hasStatus(401);
        verifyNoInteractions(checkIns);
    }

    @Test
    void countIsPublicAndHistoryIsPersonal() {
        when(checkIns.totalDeBanco(BANCO)).thenReturn(4L);
        when(checkIns.misCheckIns(Actor.user("ana"), PageQuery.first(20)))
                .thenReturn(new PageResult<>(List.of(new CheckInConBanco(CHECK_IN, "Torre del Oro")), 0, 20, 1));

        assertThat(mvc.get().uri("/api/v1/bancos/{id}/checkins/total", BANCO))
                .hasStatusOk().bodyJson().extractingPath("$.total").isEqualTo(4);
        assertThat(mvc.get().uri("/api/v1/usuarios/me/checkins")).hasStatus(401);
        assertThat(mvc.get().uri("/api/v1/usuarios/me/checkins").header("Authorization", bearer("ana")))
                .hasStatusOk().bodyJson().extractingPath("$.content[0].tituloBanco").isEqualTo("Torre del Oro");
    }
}
