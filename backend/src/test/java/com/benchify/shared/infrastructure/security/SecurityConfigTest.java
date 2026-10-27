package com.benchify.shared.infrastructure.security;

import com.benchify.shared.domain.Actor;
import com.benchify.support.ApiControllerTest;
import com.benchify.support.StubIdTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

@ApiControllerTest(SecurityConfigTest.ProbeController.class)
@Import(SecurityConfigTest.ProbeController.class)
class SecurityConfigTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void publicReadsNeedNoToken() {
        assertThat(mvc.get().uri("/api/v1/bancos")).hasStatusOk();
    }

    @Test
    void writesRequireAuthentication() {
        assertThat(mvc.post().uri("/api/v1/bancos"))
                .hasStatus(401)
                .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void personalReadsRequireAuthenticationEvenUnderPublicPaths() {
        assertThat(mvc.get().uri("/api/v1/usuarios/me")).hasStatus(401);
        assertThat(mvc.get().uri("/api/v1/usuarios/me").header("Authorization", StubIdTokenVerifier.bearer("ana")))
                .hasStatusOk()
                .bodyText().isEqualTo("ana:USER");
    }

    @Test
    void adminRoutesRequireAdminRole() {
        assertThat(mvc.get().uri("/api/v1/admin/probe").header("Authorization", StubIdTokenVerifier.bearer("ana")))
                .hasStatus(403)
                .bodyJson().extractingPath("$.code").isEqualTo("ACCESS_DENIED");
        assertThat(mvc.get().uri("/api/v1/admin/probe").header("Authorization", StubIdTokenVerifier.bearerAdmin("root")))
                .hasStatusOk();
    }

    @Test
    void invalidTokenIsRejectedEvenOnPublicRoutes() {
        assertThat(mvc.get().uri("/api/v1/bancos").header("Authorization", "Bearer forged")).hasStatus(401);
    }

    @Test
    void corsPreflightFromTheFrontendIsAllowed() {
        assertThat(mvc.options().uri("/api/v1/bancos")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST"))
                .hasStatusOk()
                .hasHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        assertThat(mvc.options().uri("/api/v1/bancos")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST"))
                .hasStatus(403);
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/v1/bancos")
        String publicRead() {
            return "ok";
        }

        @PostMapping("/api/v1/bancos")
        String write() {
            return "created";
        }

        @GetMapping("/api/v1/usuarios/me")
        String me(@CurrentActor Actor actor) {
            return actor.userId() + ":" + actor.rol();
        }

        @GetMapping("/api/v1/admin/probe")
        String admin() {
            return "admin";
        }
    }
}
