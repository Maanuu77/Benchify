package com.benchify.resena.infrastructure.web;

import com.benchify.resena.application.port.in.ResenasUseCase;
import com.benchify.resena.application.port.in.ResenasUseCase.ResenaCommand;
import com.benchify.resena.domain.Resena;
import com.benchify.resena.domain.ResenaNotFoundException;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.ConflictException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiControllerTest(ResenaController.class)
@Import(ResenaWebMapperImpl.class)
class ResenaControllerTest {

    private static final UUID BANCO = UUID.randomUUID();
    private static final Resena RESENA = new Resena(UUID.randomUUID(), BANCO, "ana", "Ana", 4, "Bonito",
            Instant.parse("2026-10-25T10:00:00Z"), null, 1, false, 0);

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private ResenasUseCase resenas;

    @Test
    void listsBenchReviewsPubliclyWithoutReportCounts() {
        when(resenas.deBanco(BANCO, null, PageQuery.first(20))).thenReturn(new PageResult<>(List.of(RESENA), 0, 20, 1));

        assertThat(mvc.get().uri("/api/v1/bancos/{id}/resenas", BANCO))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].puntuacion").isEqualTo(4);
                    assertThat(json).doesNotHavePath("$.content[0].numReportes");
                });
    }

    @Test
    void publishesReview() {
        when(resenas.publicar(Actor.user("luis"), BANCO, new ResenaCommand(5, "Genial"))).thenReturn(RESENA);

        assertThat(mvc.post().uri("/api/v1/bancos/{id}/resenas", BANCO).header("Authorization", bearer("luis"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":5,\"comentario\":\"Genial\"}"))
                .hasStatus(201)
                .hasHeader("Location", "/api/v1/resenas/" + RESENA.id());
    }

    @Test
    void validatesScore() {
        assertThat(mvc.post().uri("/api/v1/bancos/{id}/resenas", BANCO).header("Authorization", bearer("luis"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":9}"))
                .hasStatus(400)
                .bodyJson().extractingPath("$.errors[0].field").isEqualTo("puntuacion");
        verifyNoInteractions(resenas);
    }

    @Test
    void duplicateReviewIs409() {
        when(resenas.publicar(Actor.user("luis"), BANCO, new ResenaCommand(5, null)))
                .thenThrow(new ConflictException("RESENA_DUPLICADA", "Ya has reseñado este banco"));

        assertThat(mvc.post().uri("/api/v1/bancos/{id}/resenas", BANCO).header("Authorization", bearer("luis"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":5}"))
                .hasStatus(409)
                .bodyJson().extractingPath("$.code").isEqualTo("RESENA_DUPLICADA");
    }

    @Test
    void editsDeletesAndReports() {
        when(resenas.editar(Actor.user("ana"), RESENA.id(), new ResenaCommand(3, null))).thenReturn(RESENA);

        assertThat(mvc.put().uri("/api/v1/resenas/{id}", RESENA.id()).header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":3}")).hasStatusOk();
        assertThat(mvc.delete().uri("/api/v1/resenas/{id}", RESENA.id()).header("Authorization", bearer("ana")))
                .hasStatus(204);
        assertThat(mvc.post().uri("/api/v1/resenas/{id}/reportes", RESENA.id()).header("Authorization", bearer("luis")))
                .hasStatus(204);

        verify(resenas).eliminar(Actor.user("ana"), RESENA.id());
        verify(resenas).reportar(Actor.user("luis"), RESENA.id());
    }

    @Test
    void reportingRequiresAuthentication() {
        assertThat(mvc.post().uri("/api/v1/resenas/{id}/reportes", RESENA.id())).hasStatus(401);
    }

    @Test
    void theLocationOfAPublishedReviewCanBeRead() {
        when(resenas.obtener(RESENA.id(), null)).thenReturn(RESENA);
        when(resenas.obtener(RESENA.id(), Actor.user("luis"))).thenThrow(new ResenaNotFoundException(RESENA.id()));

        assertThat(mvc.get().uri("/api/v1/resenas/{id}", RESENA.id()))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.id").isEqualTo(RESENA.id().toString());
                    assertThat(json).doesNotHavePath("$.numReportes");
                });
        assertThat(mvc.get().uri("/api/v1/resenas/{id}", RESENA.id()).header("Authorization", bearer("luis")))
                .hasStatus(404).bodyJson().extractingPath("$.code").isEqualTo("RESENA_NOT_FOUND");
    }

    @Test
    void ownReviewOfABenchRequiresAuthentication() {
        when(resenas.mia(BANCO, Actor.user("ana"))).thenReturn(RESENA);
        when(resenas.mia(BANCO, Actor.user("luis"))).thenThrow(ResenaNotFoundException.sinResenaPropia(BANCO));

        assertThat(mvc.get().uri("/api/v1/bancos/{id}/resenas/mia", BANCO)).hasStatus(401);
        assertThat(mvc.get().uri("/api/v1/bancos/{id}/resenas/mia", BANCO).header("Authorization", bearer("ana")))
                .hasStatusOk().bodyJson().extractingPath("$.id").isEqualTo(RESENA.id().toString());
        assertThat(mvc.get().uri("/api/v1/bancos/{id}/resenas/mia", BANCO).header("Authorization", bearer("luis")))
                .hasStatus(404).bodyJson().extractingPath("$.code").isEqualTo("RESENA_NO_ENCONTRADA");
    }

    @Test
    void pagesBeyondTheMaximumAreRejected() {
        when(resenas.deUsuario("ana", new PageQuery(PageQuery.MAX_PAGE, 20))).thenReturn(PageResult.empty(
                new PageQuery(PageQuery.MAX_PAGE, 20)));

        assertThat(mvc.get().uri("/api/v1/usuarios/ana/resenas?page={page}", PageQuery.MAX_PAGE)).hasStatusOk();
        assertThat(mvc.get().uri("/api/v1/usuarios/ana/resenas?page={page}", PageQuery.MAX_PAGE + 1))
                .hasStatus(400)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
                    assertThat(json).extractingPath("$.errors[0].field").isEqualTo("page");
                    assertThat(json).extractingPath("$.errors[0].message")
                            .isEqualTo("La página no puede ser mayor que 10000");
                });
    }

    @Test
    void userReviewsArePublic() {
        when(resenas.deUsuario("ana", PageQuery.first(20))).thenReturn(new PageResult<>(List.of(RESENA), 0, 20, 1));

        assertThat(mvc.get().uri("/api/v1/usuarios/ana/resenas"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[0].autorId").isEqualTo("ana");
    }
}
