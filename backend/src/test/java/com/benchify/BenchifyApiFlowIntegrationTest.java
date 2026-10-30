package com.benchify;

import com.benchify.support.AbstractIntegrationTest;
import com.benchify.support.InMemoryImageStorage;
import com.benchify.support.TestImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static com.benchify.support.StubIdTokenVerifier.bearerAdmin;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Recorridos completos por la API: seguridad real (con verificador de tokens simulado),
 * Flyway, PostGIS y todas las features juntas. Cada test usa UIDs nuevos porque el alta de
 * usuarios se recuerda en memoria entre tests, aunque la transacción se deshaga.
 */
@AutoConfigureMockMvc
@Transactional
class BenchifyApiFlowIntegrationTest extends AbstractIntegrationTest {

    // Coordenadas desplazadas para no coincidir con otros datos de prueba
    private static final double LAT = -33.4489;
    private static final double LNG = -70.6693;

    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private InMemoryImageStorage storage;

    private String autora;
    private String visitante;
    private String admin;

    @BeforeEach
    void usuarios() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        autora = "autora-" + sufijo;
        visitante = "visitante-" + sufijo;
        admin = "admin-" + sufijo;
    }

    @Test
    void benchLifecycleFromProposalToReviewsFavouritesAndCheckIns() {
        String bancoId = proponerBanco();

        // Pendiente: invisible en el mapa y para otros usuarios
        assertThat(json(mvc.get().uri(mapa())).size()).isZero();
        assertThat(mvc.get().uri("/api/v1/bancos/{id}", bancoId).header("Authorization", bearer(visitante)))
                .hasStatus(404);

        // Moderación
        assertThat(mvc.get().uri("/api/v1/admin/bancos").header("Authorization", bearerAdmin(admin)))
                .hasStatusOk().bodyJson().extractingPath("$.totalElements").asNumber().isNotEqualTo(0);
        assertThat(mvc.post().uri("/api/v1/admin/bancos/{id}/aprobacion", bancoId)
                .header("Authorization", bearerAdmin(admin))).hasStatusOk();

        // Publicado: aparece en el mapa y en la búsqueda por cercanía
        assertThat(json(mvc.get().uri(mapa())).get(0).get("id").asString()).isEqualTo(bancoId);
        assertThat(mvc.get().uri("/api/v1/bancos/cercanos?lat={lat}&lng={lng}&radio=500", LAT + 0.001, LNG))
                .hasStatusOk().bodyJson().extractingPath("$.content[0].distanciaMetros").asNumber()
                .satisfies(d -> assertThat(d.intValue()).isBetween(100, 120));

        // Reseña: la autora no puede reseñar su banco; el visitante sí, una sola vez
        assertThat(mvc.post().uri("/api/v1/bancos/{id}/resenas", bancoId).header("Authorization", bearer(autora))
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":5}")).hasStatus(422);
        assertThat(mvc.post().uri("/api/v1/bancos/{id}/resenas", bancoId).header("Authorization", bearer(visitante))
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":4,\"comentario\":\"Cómodo\"}"))
                .hasStatus(201);
        assertThat(mvc.post().uri("/api/v1/bancos/{id}/resenas", bancoId).header("Authorization", bearer(visitante))
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":1}")).hasStatus(409);
        assertThat(mvc.get().uri("/api/v1/bancos/{id}", bancoId))
                .bodyJson().satisfies(banco -> {
                    assertThat(banco).extractingPath("$.puntuacionMedia").isEqualTo(4.0);
                    assertThat(banco).extractingPath("$.numResenas").isEqualTo(1);
                });

        // Favorito idempotente
        for (int i = 0; i < 2; i++) {
            assertThat(mvc.put().uri("/api/v1/bancos/{id}/favorito", bancoId)
                    .header("Authorization", bearer(visitante))).hasStatus(204);
        }
        assertThat(mvc.get().uri("/api/v1/usuarios/me/favoritos").header("Authorization", bearer(visitante)))
                .bodyJson().extractingPath("$.totalElements").isEqualTo(1);

        // Check-in: cerca vale, repetir no, lejos tampoco
        assertThat(checkIn(bancoId, LAT + 0.0005, LNG)).hasStatus(201);
        assertThat(checkIn(bancoId, LAT, LNG)).hasStatus(409)
                .bodyJson().extractingPath("$.code").isEqualTo("CHECKIN_REPETIDO");
        assertThat(mvc.get().uri("/api/v1/bancos/{id}/checkins/total", bancoId))
                .bodyJson().extractingPath("$.total").isEqualTo(1);

        // Perfil público con la actividad agregada
        assertThat(mvc.get().uri("/api/v1/usuarios/{id}/estadisticas", visitante))
                .hasStatusOk().bodyJson().satisfies(stats -> {
                    assertThat(stats).extractingPath("$.resenas").isEqualTo(1);
                    assertThat(stats).extractingPath("$.favoritos").isEqualTo(1);
                    assertThat(stats).extractingPath("$.checkIns").isEqualTo(1);
                });
        assertThat(mvc.get().uri("/api/v1/usuarios/{id}/estadisticas", autora))
                .bodyJson().extractingPath("$.bancosPublicados").isEqualTo(1);
    }

    @Test
    void reportedReviewsReachModerationAndRemovingOneRecalculatesTheRating() {
        String bancoId = bancoPublicado();
        String resenaId = json(mvc.post().uri("/api/v1/bancos/{id}/resenas", bancoId)
                .header("Authorization", bearer(visitante))
                .contentType(MediaType.APPLICATION_JSON).content("{\"puntuacion\":1,\"comentario\":\"spam spam\"}"))
                .get("id").asString();

        for (int i = 0; i < 3; i++) {
            assertThat(mvc.post().uri("/api/v1/resenas/{id}/reportes", resenaId)
                    .header("Authorization", bearer("reportero-" + i + "-" + visitante))).hasStatus(204);
        }
        assertThat(mvc.post().uri("/api/v1/resenas/{id}/reportes", resenaId)
                .header("Authorization", bearer("reportero-0-" + visitante))).hasStatus(409);

        assertThat(mvc.get().uri("/api/v1/admin/resenas/reportadas?size=100").header("Authorization", bearerAdmin(admin)))
                .bodyJson().extractingPath("$.content[*].id").asArray().contains(resenaId);

        assertThat(mvc.delete().uri("/api/v1/admin/resenas/{id}", resenaId)
                .header("Authorization", bearerAdmin(admin))).hasStatus(204);
        assertThat(mvc.get().uri("/api/v1/bancos/{id}", bancoId))
                .bodyJson().extractingPath("$.numResenas").isEqualTo(0);
    }

    @Test
    void photosAreModeratedBeforeBecomingPublic() {
        String bancoId = bancoPublicado();
        byte[] png = TestImages.png(64, 48);

        JsonNode subida = json(mvc.post().uri("/api/v1/bancos/{id}/imagenes", bancoId)
                .header("Authorization", bearer(visitante))
                .multipart().file(new MockMultipartFile("archivo", "foto.png", "image/png", png)));
        String imagenId = subida.get("id").asString();

        // Pendiente: sus ficheros no están en la carpeta pública y la API solo se los sirve a quien la subió
        assertThat(subida.get("requiereAutenticacion").asBoolean()).isTrue();
        assertThat(subida.get("url").asString())
                .isEqualTo("http://localhost/api/v1/imagenes/" + imagenId + "/archivo");
        assertThat(storage.rutas("pendientes/" + bancoId + "/")).hasSize(2);
        assertThat(storage.rutas("bancos/" + bancoId + "/")).isEmpty();
        assertThat(mvc.get().uri("/api/v1/imagenes/{id}/miniatura", imagenId)
                .header("Authorization", bearer(visitante)))
                .hasStatusOk().hasContentType("image/jpeg").hasHeader("Cache-Control", "max-age=300, private");
        assertThat(mvc.get().uri("/api/v1/imagenes/{id}/archivo", imagenId)).hasStatus(404);
        assertThat(mvc.get().uri("/api/v1/imagenes/{id}/archivo", imagenId).header("Authorization", bearer(autora)))
                .hasStatus(404);
        assertThat(json(mvc.get().uri("/api/v1/bancos/{id}/imagenes", bancoId)).size()).isZero();

        // Aprobada: los ficheros pasan a la carpeta pública y la API devuelve la URL nueva
        JsonNode aprobada = json(mvc.post().uri("/api/v1/admin/imagenes/{id}/aprobacion", imagenId)
                .header("Authorization", bearerAdmin(admin)));
        String publica = InMemoryImageStorage.BASE_URL + "bancos/" + bancoId + "/" + imagenId + ".jpg";
        assertThat(aprobada.get("requiereAutenticacion").asBoolean()).isFalse();
        assertThat(aprobada.get("url").asString()).isEqualTo(publica);
        assertThat(storage.rutas("bancos/" + bancoId + "/")).hasSize(2);
        JsonNode publicas = json(mvc.get().uri("/api/v1/bancos/{id}/imagenes", bancoId));
        assertThat(publicas.size()).isEqualTo(1);
        assertThat(publicas.get(0).get("url").asString()).isEqualTo(publica);
        assertThat(publicas.get(0).get("thumbUrl").asString()).endsWith(imagenId + "_thumb.jpg");

        assertThat(mvc.delete().uri("/api/v1/imagenes/{id}", imagenId).header("Authorization", bearer(autora)))
                .hasStatus(204);
        assertThat(json(mvc.get().uri("/api/v1/bancos/{id}/imagenes", bancoId)).size()).isZero();
        // Los ficheros se borran al confirmar cada transacción, y este test las deshace todas
        // (los borrados tras el commit se prueban en ImagenBancoServiceIntegrationTest)
    }

    @Test
    void adminPromotesAUserAndTheChangeReachesTheIdentityProvider() {
        assertThat(mvc.get().uri("/api/v1/usuarios/me").header("Authorization", bearer(visitante))).hasStatusOk();

        assertThat(mvc.put().uri("/api/v1/admin/usuarios/{id}/rol", visitante)
                .header("Authorization", bearerAdmin(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ADMIN\"}"))
                .hasStatusOk().bodyJson().extractingPath("$.rol").isEqualTo("ADMIN");
        assertThat(mvc.put().uri("/api/v1/admin/usuarios/{id}/rol", admin)
                .header("Authorization", bearerAdmin(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"USER\"}"))
                .hasStatus(422);
    }

    private String proponerBanco() {
        return json(mvc.post().uri("/api/v1/bancos").header("Authorization", bearer(autora))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"titulo":"Banco de prueba","lat":%s,"lng":%s,"accesible":true,"etiquetaIds":[1]}
                        """.formatted(LAT, LNG)))
                .get("id").asString();
    }

    private String bancoPublicado() {
        String id = proponerBanco();
        assertThat(mvc.post().uri("/api/v1/admin/bancos/{id}/aprobacion", id)
                .header("Authorization", bearerAdmin(admin))).hasStatusOk();
        return id;
    }

    private MvcTestResult checkIn(String bancoId, double lat, double lng) {
        return mvc.post().uri("/api/v1/bancos/{id}/checkins", bancoId).header("Authorization", bearer(visitante))
                .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lng\":%s}".formatted(lat, lng))
                .exchange();
    }

    private static String mapa() {
        return "/api/v1/bancos?minLat=%s&minLng=%s&maxLat=%s&maxLng=%s"
                .formatted(LAT - 0.01, LNG - 0.01, LAT + 0.01, LNG + 0.01);
    }

    private JsonNode json(MockMvcTester.MockMvcRequestBuilder request) {
        return json(request.exchange());
    }

    private JsonNode json(MockMvcTester.MockMultipartMvcRequestBuilder request) {
        return json(request.exchange());
    }

    private JsonNode json(MvcTestResult result) {
        assertThat(result).hasStatus2xxSuccessful();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private JsonNode json(String uri) {
        return json(mvc.get().uri(uri));
    }
}
