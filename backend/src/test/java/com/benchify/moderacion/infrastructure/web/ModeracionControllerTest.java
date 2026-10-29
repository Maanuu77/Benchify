package com.benchify.moderacion.infrastructure.web;

import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.EstadoBanco;
import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Etiqueta;
import com.benchify.banco.domain.Imagen;
import com.benchify.moderacion.application.port.in.ModeracionUseCase;
import com.benchify.moderacion.domain.EstadisticasModeracion;
import com.benchify.resena.domain.Resena;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.support.ApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static com.benchify.support.StubIdTokenVerifier.bearerAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiControllerTest(ModeracionController.class)
@Import(ModeracionWebMapperImpl.class)
class ModeracionControllerTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");
    private static final Actor ADMIN = Actor.admin("root");
    private static final Banco BANCO = Banco.proponer(UUID.randomUUID(),
            new DatosBanco("Pendiente", "Junto a la fuente", new Ubicacion(37, -6), null, null,
                    Set.of(new Etiqueta(1L, "relax", "#3498DB", "spa"))),
            Actor.user("ana"), AHORA);

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private ModeracionUseCase moderacion;

    @Test
    void adminRoutesAreForbiddenToRegularUsers() {
        assertThat(mvc.get().uri("/api/v1/admin/estadisticas").header("Authorization", bearer("ana"))).hasStatus(403);
        assertThat(mvc.get().uri("/api/v1/admin/estadisticas")).hasStatus(401);
        verifyNoInteractions(moderacion);
    }

    @Test
    void returnsDashboardStatistics() {
        when(moderacion.estadisticas(ADMIN)).thenReturn(new EstadisticasModeracion(3, 40, 2, 1, 5, 120, 33));

        assertThat(mvc.get().uri("/api/v1/admin/estadisticas").header("Authorization", bearerAdmin("root")))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.bancosPendientes").isEqualTo(3);
                    assertThat(json).extractingPath("$.totalUsuarios").isEqualTo(33);
                });
    }

    @Test
    void listsPendingBenchesByDefaultAndValidatesTheState() {
        when(moderacion.bancos(ADMIN, EstadoBanco.PENDIENTE, PageQuery.first(20)))
                .thenReturn(new PageResult<>(List.of(BANCO), 0, 20, 1));

        assertThat(mvc.get().uri("/api/v1/admin/bancos").header("Authorization", bearerAdmin("root")))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].autorId").isEqualTo("ana");
                    assertThat(json).extractingPath("$.content[0].etiquetas[0]").isEqualTo("relax");
                });
        assertThat(mvc.get().uri("/api/v1/admin/bancos?estado=BORRADOR").header("Authorization", bearerAdmin("root")))
                .hasStatus(400);
    }

    @Test
    void approvesAndRejectsBenches() {
        when(moderacion.aprobarBanco(ADMIN, BANCO.id())).thenReturn(BANCO);
        when(moderacion.rechazarBanco(ADMIN, BANCO.id())).thenReturn(BANCO);

        assertThat(mvc.post().uri("/api/v1/admin/bancos/{id}/aprobacion", BANCO.id())
                .header("Authorization", bearerAdmin("root"))).hasStatusOk();
        assertThat(mvc.post().uri("/api/v1/admin/bancos/{id}/rechazo", BANCO.id())
                .header("Authorization", bearerAdmin("root"))).hasStatusOk();
    }

    @Test
    void moderatesReportedReviews() {
        Resena reportada = new Resena(UUID.randomUUID(), BANCO.id(), "luis", "Luis", 1, "Spam", AHORA, null, 3, true, 0);
        Resena revisada = new Resena(reportada.id(), BANCO.id(), "luis", "Luis", 1, "Spam", AHORA, null, 0, false, 0);
        when(moderacion.resenasReportadas(ADMIN, PageQuery.first(20)))
                .thenReturn(new PageResult<>(List.of(reportada), 0, 20, 1));
        when(moderacion.mantenerResena(ADMIN, reportada.id())).thenReturn(revisada);

        assertThat(mvc.get().uri("/api/v1/admin/resenas/reportadas").header("Authorization", bearerAdmin("root")))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[0].numReportes").isEqualTo(3);
        assertThat(mvc.post().uri("/api/v1/admin/resenas/{id}/revision", reportada.id())
                .header("Authorization", bearerAdmin("root")))
                .hasStatusOk()
                .bodyJson().extractingPath("$.numReportes").isEqualTo(0);
        assertThat(mvc.delete().uri("/api/v1/admin/resenas/{id}", reportada.id())
                .header("Authorization", bearerAdmin("root"))).hasStatus(204);
        verify(moderacion).eliminarResena(ADMIN, reportada.id());
    }

    @Test
    void moderatesImages() {
        Imagen imagen = new Imagen(UUID.randomUUID(), BANCO.id(), "bancos/x.jpg", "https://storage/x.jpg",
                "image/jpeg", 2048, "ana", EstadoImagen.PENDIENTE, AHORA);
        when(moderacion.imagenesPendientes(ADMIN, PageQuery.first(20)))
                .thenReturn(new PageResult<>(List.of(imagen), 0, 20, 1));
        when(moderacion.aprobarImagen(ADMIN, imagen.id())).thenReturn(imagen.aprobar());

        assertThat(mvc.get().uri("/api/v1/admin/imagenes/pendientes").header("Authorization", bearerAdmin("root")))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[0].tamanoBytes").isEqualTo(2048);
        assertThat(mvc.post().uri("/api/v1/admin/imagenes/{id}/aprobacion", imagen.id())
                .header("Authorization", bearerAdmin("root")))
                .hasStatusOk()
                .bodyJson().extractingPath("$.estado").isEqualTo("APROBADA");
        assertThat(mvc.post().uri("/api/v1/admin/imagenes/{id}/rechazo", imagen.id())
                .header("Authorization", bearerAdmin("root"))).hasStatus(204);
        verify(moderacion).rechazarImagen(ADMIN, imagen.id());
    }
}
