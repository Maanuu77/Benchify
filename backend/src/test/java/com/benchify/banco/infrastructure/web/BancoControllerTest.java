package com.benchify.banco.infrastructure.web;

import com.benchify.banco.application.port.in.BancoCommand;
import com.benchify.banco.application.port.in.ConsultarBancosUseCase;
import com.benchify.banco.application.port.in.GestionarBancoUseCase;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.BancoCercano;
import com.benchify.banco.domain.BancoNotFoundException;
import com.benchify.banco.domain.BoundingBox;
import com.benchify.banco.domain.Caracteristicas;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.DireccionPostal;
import com.benchify.banco.domain.Etiqueta;
import com.benchify.banco.domain.FiltroBancos;
import com.benchify.banco.domain.MaterialBanco;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.ForbiddenException;
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
import java.util.Set;
import java.util.UUID;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiControllerTest({BancoController.class, EtiquetaController.class, UsuarioBancosController.class})
@Import(BancoWebMapperImpl.class)
class BancoControllerTest {

    private static final Etiqueta VISTAS = new Etiqueta(3L, "vistas", "#2ECC71", "landscape");
    private static final Etiqueta RELAX = new Etiqueta(2L, "relax", "#3498DB", "spa");
    private static final Banco BANCO = Banco.proponer(UUID.fromString("7d7b8b0e-1111-4c1a-9f00-000000000001"),
            new DatosBanco("Banco del Betis", "Vistas a Triana", new Ubicacion(37.3826, -6.0034),
                    new DireccionPostal("Calle Betis", "Sevilla", "Sevilla", null),
                    new Caracteristicas(true, false, true, MaterialBanco.MADERA), Set.of(VISTAS, RELAX)),
            Actor.user("ana"), Instant.parse("2026-10-25T10:00:00Z"));

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private ConsultarBancosUseCase consultarBancos;
    @MockitoBean
    private GestionarBancoUseCase gestionarBanco;

    @Test
    void mapQueryReturnsLightweightMarkers() {
        when(consultarBancos.enArea(new BoundingBox(37.3, -6.1, 37.4, -5.9),
                new FiltroBancos(true, null, null, 3L, 4.0))).thenReturn(List.of(BANCO));

        assertThat(mvc.get().uri("/api/v1/bancos?minLat=37.3&minLng=-6.1&maxLat=37.4&maxLng=-5.9"
                + "&accesible=true&etiqueta=3&puntuacionMin=4"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[0].titulo").isEqualTo("Banco del Betis");
                    assertThat(json).extractingPath("$[0].etiquetas").asArray().containsExactly("relax", "vistas");
                    assertThat(json).doesNotHavePath("$[0].descripcion");
                });
    }

    @Test
    void invertedBoundingBoxIsUnprocessable() {
        assertThat(mvc.get().uri("/api/v1/bancos?minLat=37.4&minLng=-6.1&maxLat=37.3&maxLng=-5.9"))
                .hasStatus(422)
                .bodyJson().extractingPath("$.code").isEqualTo("BOUNDING_BOX_INVALIDA");
    }

    @Test
    void outOfRangeCoordinatesAreBadRequest() {
        assertThat(mvc.get().uri("/api/v1/bancos?minLat=-95&minLng=-6.1&maxLat=37.3&maxLng=-5.9")).hasStatus(400);
        assertThat(mvc.get().uri("/api/v1/bancos?minLng=-6.1&maxLat=37.3&maxLng=-5.9")).hasStatus(400);
    }

    @Test
    void nearbySearchReturnsDistanceAndPagination() {
        when(consultarBancos.cercanos(new Ubicacion(37.38, -6.0), 2000, FiltroBancos.NINGUNO, new PageQuery(0, 5)))
                .thenReturn(new PageResult<>(List.of(new BancoCercano(BANCO, 412.6)), 0, 5, 1));

        assertThat(mvc.get().uri("/api/v1/bancos/cercanos?lat=37.38&lng=-6.0&radio=2000&size=5"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].distanciaMetros").isEqualTo(413);
                    assertThat(json).extractingPath("$.content[0].banco.id").isEqualTo(BANCO.id().toString());
                    assertThat(json).extractingPath("$.totalElements").isEqualTo(1);
                });
    }

    @Test
    void nearbySearchRejectsHugeRadius() {
        assertThat(mvc.get().uri("/api/v1/bancos/cercanos?lat=37.38&lng=-6.0&radio=100000")).hasStatus(400);
    }

    @Test
    void detailIsPublicAndPassesTheOptionalActor() {
        when(consultarBancos.obtener(BANCO.id(), null)).thenReturn(BANCO);
        when(consultarBancos.obtener(BANCO.id(), Actor.user("ana"))).thenReturn(BANCO);

        assertThat(mvc.get().uri("/api/v1/bancos/{id}", BANCO.id()))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.autor.id").isEqualTo("ana");
                    assertThat(json).extractingPath("$.estado").isEqualTo("PENDIENTE");
                    assertThat(json).extractingPath("$.pais").isEqualTo("España");
                    assertThat(json).extractingPath("$.etiquetas[0].nombre").isEqualTo("relax");
                });
        assertThat(mvc.get().uri("/api/v1/bancos/{id}", BANCO.id()).header("Authorization", bearer("ana")))
                .hasStatusOk();
    }

    @Test
    void unknownBenchIs404() {
        UUID id = UUID.randomUUID();
        when(consultarBancos.obtener(id, null)).thenThrow(new BancoNotFoundException(id));

        assertThat(mvc.get().uri("/api/v1/bancos/{id}", id)).hasStatus(404)
                .bodyJson().extractingPath("$.code").isEqualTo("BANCO_NOT_FOUND");
    }

    @Test
    void proposingABenchReturns201WithLocation() {
        when(gestionarBanco.proponer(eq(Actor.user("ana")), any(BancoCommand.class))).thenReturn(BANCO);

        assertThat(mvc.post().uri("/api/v1/bancos").header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"titulo":"Banco del Betis","lat":37.3826,"lng":-6.0034,"conSombra":true,
                         "material":"MADERA","etiquetaIds":[2,3]}
                        """))
                .hasStatus(201)
                .hasHeader("Location", "http://localhost/api/v1/bancos/" + BANCO.id());
        verify(gestionarBanco).proponer(Actor.user("ana"), new BancoCommand("Banco del Betis", null, 37.3826,
                -6.0034, null, null, null, null, true, false, false, MaterialBanco.MADERA, Set.of(2L, 3L)));
    }

    @Test
    void proposingRequiresAuthenticationAndValidBody() {
        assertThat(mvc.post().uri("/api/v1/bancos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(401);
        assertThat(mvc.post().uri("/api/v1/bancos").header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"\",\"lat\":100}"))
                .hasStatus(400)
                .bodyJson().extractingPath("$.errors").asArray().hasSize(3);
        verifyNoInteractions(gestionarBanco);
    }

    @Test
    void editingSomeoneElsesBenchIsForbidden() {
        when(gestionarBanco.editar(eq(Actor.user("luis")), eq(BANCO.id()), any()))
                .thenThrow(new ForbiddenException("BANCO_NO_AUTOR", "No eres el autor"));

        assertThat(mvc.put().uri("/api/v1/bancos/{id}", BANCO.id()).header("Authorization", bearer("luis"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"x\",\"lat\":1,\"lng\":1}"))
                .hasStatus(403);
    }

    @Test
    void deleteReturns204() {
        assertThat(mvc.delete().uri("/api/v1/bancos/{id}", BANCO.id()).header("Authorization", bearer("ana")))
                .hasStatus(204);
        verify(gestionarBanco).eliminar(Actor.user("ana"), BANCO.id());
    }

    @Test
    void tagCatalogIsCacheable() {
        when(consultarBancos.etiquetas()).thenReturn(List.of(RELAX, VISTAS));

        assertThat(mvc.get().uri("/api/v1/etiquetas"))
                .hasStatusOk()
                .hasHeader("Cache-Control", "max-age=3600, public")
                .bodyJson().extractingPath("$[1].color").isEqualTo("#2ECC71");
    }

    @Test
    void userBenchesArePublicAndPaginated() {
        when(consultarBancos.deUsuario("ana", null, PageQuery.first(20)))
                .thenReturn(new PageResult<>(List.of(BANCO), 0, 20, 1));

        assertThat(mvc.get().uri("/api/v1/usuarios/ana/bancos"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.content[0].estado").isEqualTo("PENDIENTE");
    }
}
