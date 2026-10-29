package com.benchify.shared.infrastructure.web;

import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ConflictException;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final MockMvcTester mvc = MockMvcTester.create(MockMvcBuilders
            .standaloneSetup(new FailingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build());

    @Test
    void mapsNotFoundToProblemDetailWithCode() {
        assertThat(mvc.get().uri("/not-found"))
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.status").isEqualTo(404);
                    assertThat(json).extractingPath("$.code").isEqualTo("BANCO_NOT_FOUND");
                    assertThat(json).extractingPath("$.detail").isEqualTo("Banco no encontrado");
                    assertThat(json).extractingPath("$.instance").isEqualTo("/not-found");
                });
    }

    @Test
    void mapsDomainExceptionsToTheirStatus() {
        assertThat(mvc.get().uri("/conflict")).hasStatus(409);
        assertThat(mvc.get().uri("/forbidden")).hasStatus(403);
        assertThat(mvc.get().uri("/rule")).hasStatus(422)
                .bodyJson().extractingPath("$.code").isEqualTo("BANCO_NOT_PUBLISHED");
    }

    @Test
    void mapsSpringSecurityAccessDeniedTo403() {
        assertThat(mvc.get().uri("/access-denied")).hasStatus(403)
                .bodyJson().extractingPath("$.code").isEqualTo("ACCESS_DENIED");
    }

    @Test
    void listsInvalidBodyFields() {
        assertThat(mvc.post().uri("/body").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .hasStatus(400)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
                    assertThat(json).extractingPath("$.errors[0].field").isEqualTo("nombre");
                });
    }

    @Test
    void listsInvalidRequestParameters() {
        assertThat(mvc.get().uri("/param").param("size", "500"))
                .hasStatus(400)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
                    assertThat(json).extractingPath("$.errors[0].field").isEqualTo("size");
                });
    }

    @Test
    void addsCodeToSpringMvcErrors() {
        assertThat(mvc.post().uri("/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .hasStatus(400)
                .bodyJson().extractingPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void malformedMultipartIsBadRequestButOversizedUploadIs413() {
        assertThat(mvc.get().uri("/multipart"))
                .hasStatus(400)
                .bodyJson().extractingPath("$.code").isEqualTo("MULTIPART_INVALIDO");
        assertThat(mvc.get().uri("/too-big"))
                .hasStatus(413)
                .bodyJson().extractingPath("$.code").isEqualTo("CONTENT_TOO_LARGE");
    }

    @Test
    void concurrentUpdatesAreConflicts() {
        assertThat(mvc.get().uri("/stale")).hasStatus(409)
                .bodyJson().extractingPath("$.code").isEqualTo("CONFLICTO_CONCURRENTE");
    }

    @Test
    void databaseConstraintViolationsAreConflictsWithoutSqlDetails() {
        assertThat(mvc.get().uri("/integrity"))
                .hasStatus(409)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.code").isEqualTo("CONFLICTO_DATOS");
                    assertThat(json).extractingPath("$.detail").asString().doesNotContain("usuarios_pkey");
                });
    }

    @Test
    void hidesUnexpectedErrorDetails() {
        assertThat(mvc.get().uri("/boom"))
                .hasStatus(500)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.code").isEqualTo("INTERNAL_ERROR");
                    assertThat(json).extractingPath("$.detail").isEqualTo("Se ha producido un error inesperado");
                });
    }

    record Body(@NotBlank String nombre) {
    }

    @RestController
    static class FailingController {

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("BANCO_NOT_FOUND", "Banco no encontrado");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("RESENA_DUPLICATED", "Ya has reseñado este banco");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenException("BANCO_NOT_OWNER", "No eres el autor");
        }

        @GetMapping("/rule")
        void rule() {
            throw new BusinessRuleException("BANCO_NOT_PUBLISHED", "El banco no está publicado");
        }

        @GetMapping("/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("denied");
        }

        @PostMapping("/body")
        void body(@Valid @RequestBody Body body) {
        }

        @GetMapping("/param")
        void param(@RequestParam @Max(100) int size) {
        }

        @GetMapping("/multipart")
        void multipart() {
            throw new MultipartException("Failed to parse multipart servlet request");
        }

        @GetMapping("/too-big")
        void tooBig() {
            throw new MaxUploadSizeExceededException(10 * 1024 * 1024);
        }

        @GetMapping("/stale")
        void stale() {
            throw new OptimisticLockingFailureException("version mismatch");
        }

        @GetMapping("/integrity")
        void integrity() {
            throw new DuplicateKeyException(
                    "duplicate key value violates unique constraint \"usuarios_pkey\"");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("secret internals");
        }
    }
}
