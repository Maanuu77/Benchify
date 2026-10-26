package com.benchify.shared.infrastructure.web;

import com.benchify.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
class OpenApiDocsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void documentsEveryFeatureWithFirebaseSecurityAndProblemResponses() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.info.title").isEqualTo("Benchify API");
                    assertThat(json).extractingPath("$.components.securitySchemes.firebase.scheme").isEqualTo("bearer");
                    assertThat(json).hasPath("$.paths['/api/v1/bancos/cercanos'].get");
                    assertThat(json).hasPath("$.paths['/api/v1/bancos/{bancoId}/resenas'].post");
                    assertThat(json).hasPath("$.paths['/api/v1/admin/estadisticas'].get");
                    assertThat(json).hasPath("$.paths['/api/v1/usuarios/me/favoritos'].get");
                    assertThat(json).hasPath("$.paths['/api/v1/bancos/{bancoId}/checkins'].post.responses['422']");
                    assertThat(json).hasPath("$.paths['/api/v1/usuarios/me'].get.responses['401']");
                    // El actor autenticado no aparece como parámetro de la API
                    assertThat(json).doesNotHavePath("$.paths['/api/v1/usuarios/me'].get.parameters");
                });
    }

    @Test
    void swaggerUiIsServedWithoutAuthentication() {
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
    }
}
