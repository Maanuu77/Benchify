package com.benchify.incidencia.infrastructure.web;

import com.benchify.incidencia.application.port.in.IncidenciasUseCase;
import com.benchify.incidencia.domain.EstadoIncidencia;
import com.benchify.incidencia.domain.Incidencia;
import com.benchify.incidencia.domain.TipoIncidencia;
import com.benchify.shared.domain.Actor;
import com.benchify.support.ApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static com.benchify.support.StubIdTokenVerifier.bearerAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiControllerTest(IncidenciaController.class)
class IncidenciaControllerTest {

    private static final UUID BANCO = UUID.randomUUID();

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private IncidenciasUseCase incidencias;

    @Test
    void reportsAProblem() {
        Incidencia creada = Incidencia.reportar(BANCO, Actor.user("ana"), TipoIncidencia.ROTO, "Tabla rota", Instant.now());
        when(incidencias.reportar(Actor.user("ana"), BANCO, TipoIncidencia.ROTO, "Tabla rota")).thenReturn(creada);

        assertThat(mvc.post().uri("/api/v1/bancos/{id}/incidencias", BANCO).header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"ROTO\",\"comentario\":\"Tabla rota\"}"))
                .hasStatus(201)
                .bodyJson().extractingPath("$.estado").isEqualTo("ABIERTA");
    }

    @Test
    void rejectsAMissingTypeAndAnonymousReports() {
        assertThat(mvc.post().uri("/api/v1/bancos/{id}/incidencias", BANCO).header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(400);
        assertThat(mvc.post().uri("/api/v1/bancos/{id}/incidencias", BANCO)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"ROTO\"}"))
                .hasStatus(401);
        verifyNoInteractions(incidencias);
    }

    @Test
    void summaryIsPublic() {
        when(incidencias.abiertasPorTipo(eq(BANCO), any())).thenReturn(Map.of(TipoIncidencia.SUCIO, 2L));

        assertThat(mvc.get().uri("/api/v1/bancos/{id}/incidencias/resumen", BANCO))
                .hasStatusOk()
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$[0].tipo").isEqualTo("SUCIO");
                    assertThat(json).extractingPath("$[0].total").isEqualTo(2);
                });
    }

    @Test
    void adminsCloseIncidents() {
        Incidencia abierta = Incidencia.reportar(BANCO, Actor.user("ana"), TipoIncidencia.ROTO, null, Instant.now());
        Actor admin = Actor.admin("root");
        when(incidencias.cerrar(admin, abierta.id(), EstadoIncidencia.RESUELTA))
                .thenReturn(abierta.cerrar(admin, EstadoIncidencia.RESUELTA, Instant.now()));

        assertThat(mvc.post().uri("/api/v1/admin/incidencias/{id}/resolucion", abierta.id())
                .header("Authorization", bearerAdmin("root"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"RESUELTA\"}"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.estado").isEqualTo("RESUELTA");
        assertThat(mvc.post().uri("/api/v1/admin/incidencias/{id}/resolucion", abierta.id())
                .header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"RESUELTA\"}"))
                .hasStatus(403);
    }
}
