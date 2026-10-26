package com.benchify.favorito.infrastructure.web;

import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.DireccionPostal;
import com.benchify.banco.domain.Ubicacion;
import com.benchify.favorito.application.port.in.FavoritoConBanco;
import com.benchify.favorito.application.port.in.FavoritosUseCase;
import com.benchify.favorito.domain.Favorito;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.support.ApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiControllerTest(FavoritoController.class)
@Import(FavoritoWebMapperImpl.class)
class FavoritoControllerTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");
    private static final Banco BANCO = Banco.proponer(UUID.randomUUID(),
            new DatosBanco("Glorieta", null, new Ubicacion(37.37, -5.98),
                    new DireccionPostal(null, "Sevilla", null, null), null, null),
            Actor.admin("root"), AHORA);

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private FavoritosUseCase favoritos;

    @Test
    void marksAndUnmarksFavourite() {
        assertThat(mvc.put().uri("/api/v1/bancos/{id}/favorito", BANCO.id()).header("Authorization", bearer("ana")))
                .hasStatus(204);
        assertThat(mvc.delete().uri("/api/v1/bancos/{id}/favorito", BANCO.id()).header("Authorization", bearer("ana")))
                .hasStatus(204);

        verify(favoritos).marcar(Actor.user("ana"), BANCO.id());
        verify(favoritos).desmarcar(Actor.user("ana"), BANCO.id());
    }

    @Test
    void favouriteStateRequiresAuthentication() {
        when(favoritos.esFavorito(Actor.user("ana"), BANCO.id())).thenReturn(true);

        assertThat(mvc.get().uri("/api/v1/bancos/{id}/favorito", BANCO.id())).hasStatus(401);
        assertThat(mvc.get().uri("/api/v1/bancos/{id}/favorito", BANCO.id()).header("Authorization", bearer("ana")))
                .hasStatusOk()
                .bodyJson().extractingPath("$.favorito").isEqualTo(true);
    }

    @Test
    void listsMyFavouritesWithBenchSummary() {
        when(favoritos.misFavoritos(Actor.user("ana"), PageQuery.first(20))).thenReturn(new PageResult<>(
                List.of(new FavoritoConBanco(new Favorito("ana", BANCO.id(), AHORA), BANCO)), 0, 20, 1));

        assertThat(mvc.get().uri("/api/v1/usuarios/me/favoritos").header("Authorization", bearer("ana")))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].titulo").isEqualTo("Glorieta");
                    assertThat(json).extractingPath("$.content[0].ciudad").isEqualTo("Sevilla");
                    assertThat(json).extractingPath("$.content[0].bancoId").isEqualTo(BANCO.id().toString());
                });
    }
}
