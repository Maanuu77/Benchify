package com.benchify.shared.infrastructure.security;

import com.benchify.shared.domain.Rol;
import com.benchify.support.StubIdTokenVerifier;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FirebaseAuthenticationFilterTest {

    private final FirebaseAuthenticationFilter filter = new FirebaseAuthenticationFilter(
            new StubIdTokenVerifier(), new ProblemDetailSecurityHandler(JsonMapper.builder().build()));

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final AtomicReference<Authentication> seenByChain = new AtomicReference<>();
    private boolean chainCalled;

    private final FilterChain chain = (req, res) -> {
        chainCalled = true;
        seenByChain.set(SecurityContextHolder.getContext().getAuthentication());
    };

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void continuesAnonymouslyWithoutAuthorizationHeader() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/bancos"), response, chain);

        assertThat(chainCalled).isTrue();
        assertThat(seenByChain.get()).isNull();
    }

    @Test
    void ignoresNonBearerSchemes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bancos");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertThat(chainCalled).isTrue();
        assertThat(seenByChain.get()).isNull();
    }

    @Test
    void authenticatesValidTokenWithRoleFromClaims() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/estadisticas");
        request.addHeader("Authorization", StubIdTokenVerifier.bearerAdmin("ana"));

        filter.doFilter(request, response, chain);

        Authentication authentication = seenByChain.get();
        assertThat(authentication).isInstanceOf(FirebaseAuthentication.class);
        assertThat(authentication.getName()).isEqualTo("ana");
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        assertThat(((AuthenticatedUser) authentication.getPrincipal()).rol()).isEqualTo(Rol.ADMIN);
        assertThat(authentication.getCredentials()).isNull();
    }

    @Test
    void rejectsInvalidTokenWithProblemDetail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/usuarios/me");
        request.addHeader("Authorization", "Bearer forged");

        filter.doFilter(request, response, chain);

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHENTICATED\"")
                .contains("\"instance\":\"/api/v1/usuarios/me\"");
    }

    @Test
    void rejectsEmptyBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/usuarios/me");
        request.addHeader("Authorization", "Bearer   ");

        filter.doFilter(request, response, chain);

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
