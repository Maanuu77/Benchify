package com.benchify.shared.infrastructure.ratelimit;

import com.benchify.support.ApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static org.assertj.core.api.Assertions.assertThat;

/** El limitador registrado detrás de la cadena de seguridad real (#26). */
@ApiControllerTest(RateLimitConfigTest.ProbeController.class)
@Import({RateLimitConfig.class, RateLimitConfigTest.ProbeController.class})
@TestPropertySource(properties = {
        "benchify.rate-limit.enabled=true",
        "benchify.rate-limit.writes.capacity=2",
        "benchify.rate-limit.writes.period=1h",
        "benchify.rate-limit.uploads.capacity=1",
        "benchify.rate-limit.uploads.period=1h"
})
class RateLimitConfigTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void authenticatedWritesAreLimitedPerUser() {
        for (int i = 0; i < 2; i++) {
            assertThat(mvc.post().uri("/api/v1/bancos").header("Authorization", bearer("ana"))).hasStatusOk();
        }

        assertThat(mvc.post().uri("/api/v1/bancos").header("Authorization", bearer("ana")))
                .hasStatus(429)
                .hasHeader("Retry-After", "1800")
                .bodyJson().extractingPath("$.code").isEqualTo("DEMASIADAS_PETICIONES");
        assertThat(mvc.post().uri("/api/v1/bancos").header("Authorization", bearer("luis"))).hasStatusOk();
    }

    @Test
    void photoUploadsUseTheirOwnQuota() {
        assertThat(mvc.post().uri("/api/v1/bancos/1/imagenes").header("Authorization", bearer("eva"))).hasStatusOk();
        assertThat(mvc.post().uri("/api/v1/bancos/1/imagenes").header("Authorization", bearer("eva")))
                .hasStatus(429);
        assertThat(mvc.post().uri("/api/v1/bancos").header("Authorization", bearer("eva"))).hasStatusOk();
    }

    @Test
    void theSpaCanReadRetryAfterOnACrossOrigin429() {
        assertThat(mvc.post().uri("/api/v1/bancos/2/imagenes").header("Authorization", bearer("sara"))
                .header("Origin", "http://localhost:4200")).hasStatusOk();

        assertThat(mvc.post().uri("/api/v1/bancos/2/imagenes").header("Authorization", bearer("sara"))
                .header("Origin", "http://localhost:4200"))
                .hasStatus(429)
                .hasHeader("Access-Control-Allow-Origin", "http://localhost:4200")
                .headers().hasValue("Access-Control-Expose-Headers", "Location, ETag, Retry-After");
    }

    @Test
    void requestsRejectedBySecurityDoNotSpendTheQuota() {
        for (int i = 0; i < 5; i++) {
            assertThat(mvc.post().uri("/api/v1/bancos")).hasStatus(401);
            assertThat(mvc.post().uri("/api/v1/admin/probe").header("Authorization", bearer("raul"))).hasStatus(403);
        }

        assertThat(mvc.post().uri("/api/v1/bancos").header("Authorization", bearer("raul"))).hasStatusOk();
    }

    @RestController
    static class ProbeController {

        @PostMapping("/api/v1/bancos")
        String crear() {
            return "ok";
        }

        @PostMapping("/api/v1/bancos/{id}/imagenes")
        String subir() {
            return "ok";
        }

        @PostMapping("/api/v1/admin/probe")
        String admin() {
            return "ok";
        }
    }
}
