package com.benchify.shared.infrastructure.ratelimit;

import com.benchify.shared.domain.Rol;
import com.benchify.shared.infrastructure.ratelimit.RateLimitProperties.Limit;
import com.benchify.shared.infrastructure.security.AuthenticatedUser;
import com.benchify.shared.infrastructure.security.FirebaseAuthentication;
import com.benchify.shared.infrastructure.security.ProblemDetailSecurityHandler;
import io.github.bucket4j.TimeMeter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.ForwardedHeaderFilter;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static final Limit TRES_POR_MINUTO = new Limit(3, Duration.ofMinutes(1));
    private static final Limit UNA_CADA_DIEZ_MINUTOS = new Limit(1, Duration.ofMinutes(10));

    private static final HttpServlet CONTROLADOR = new HttpServlet() {
        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) {
            response.setStatus(200);
        }
    };

    private final AtomicLong nanos = new AtomicLong();
    private final TimeMeter reloj = new TimeMeter() {
        @Override
        public long currentTimeNanos() {
            return nanos.get();
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }
    };

    private RateLimitFilter filter = filtro(100);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void writesBeyondTheCapacityAreRejectedWithRetryAfter() throws Exception {
        autenticar("ana");
        for (int i = 0; i < 3; i++) {
            assertThat(enviar("POST", "/api/v1/bancos").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse rechazada = enviar("PUT", "/api/v1/resenas/1");

        assertThat(rechazada.getStatus()).isEqualTo(429);
        assertThat(rechazada.getContentType()).startsWith("application/problem+json");
        // Un token cada 20 s
        assertThat(rechazada.getHeader("Retry-After")).isEqualTo("20");
        assertThat(rechazada.getContentAsString())
                .contains("\"code\":\"DEMASIADAS_PETICIONES\"")
                .contains("\"status\":429")
                .contains("\"instance\":\"/api/v1/resenas/1\"");
    }

    @Test
    void tokensComeBackOverTime() throws Exception {
        autenticar("ana");
        for (int i = 0; i < 3; i++) {
            enviar("DELETE", "/api/v1/resenas/1");
        }
        assertThat(enviar("PATCH", "/api/v1/usuarios/me").getStatus()).isEqualTo(429);

        avanzar(Duration.ofSeconds(20));

        assertThat(enviar("PATCH", "/api/v1/usuarios/me").getStatus()).isEqualTo(200);
        assertThat(enviar("PATCH", "/api/v1/usuarios/me").getStatus()).isEqualTo(429);
    }

    @Test
    void readsAndRequestsOutsideTheApiAreNotLimited() throws Exception {
        autenticar("ana");
        for (int i = 0; i < 10; i++) {
            assertThat(enviar("GET", "/api/v1/bancos").getStatus()).isEqualTo(200);
            assertThat(enviar("OPTIONS", "/api/v1/bancos").getStatus()).isEqualTo(200);
            assertThat(enviar("POST", "/actuator/health").getStatus()).isEqualTo(200);
        }
        assertThat(filter.clavesRecordadas()).isZero();
    }

    @Test
    void eachUserHasItsOwnBucket() throws Exception {
        autenticar("ana");
        for (int i = 0; i < 3; i++) {
            enviar("POST", "/api/v1/bancos");
        }
        assertThat(enviar("POST", "/api/v1/bancos").getStatus()).isEqualTo(429);

        autenticar("luis");
        assertThat(enviar("POST", "/api/v1/bancos").getStatus()).isEqualTo(200);
    }

    @Test
    void photoUploadsHaveTheirOwnStricterBucket() throws Exception {
        autenticar("ana");
        assertThat(enviar("POST", "/api/v1/bancos/42/imagenes").getStatus()).isEqualTo(200);

        MockHttpServletResponse rechazada = enviar("POST", "/api/v1/bancos/42/imagenes");
        assertThat(rechazada.getStatus()).isEqualTo(429);
        assertThat(rechazada.getHeader("Retry-After")).isEqualTo("600");

        // Las demás escrituras siguen teniendo su cupo, incluida la moderación de imágenes
        assertThat(enviar("POST", "/api/v1/bancos/42/resenas").getStatus()).isEqualTo(200);
        assertThat(enviar("POST", "/api/v1/admin/imagenes/7/aprobacion").getStatus()).isEqualTo(200);
    }

    @Test
    void anonymousRequestsAreLimitedByTheFirstForwardedClientAddress() throws Exception {
        // En producción, server.forward-headers-strategy=framework antepone este filtro a la cadena
        ForwardedHeaderFilter forwarded = new ForwardedHeaderFilter();
        for (int i = 0; i < 3; i++) {
            assertThat(enviar(peticionDesde("203.0.113.7, 10.0.0.1"), forwarded).getStatus()).isEqualTo(200);
        }

        assertThat(enviar(peticionDesde("203.0.113.7, 10.0.0.2"), forwarded).getStatus()).isEqualTo(429);
        assertThat(enviar(peticionDesde("198.51.100.4, 10.0.0.1"), forwarded).getStatus()).isEqualTo(200);
    }

    @Test
    void remembersABoundedNumberOfKeys() throws Exception {
        filter = filtro(5);

        for (int i = 0; i < 50; i++) {
            autenticar("usuario-" + i);
            enviar("POST", "/api/v1/bancos");
        }

        assertThat(filter.clavesRecordadas()).isLessThanOrEqualTo(5);
    }

    @Test
    void propertiesFallBackToTheDefaultLimits() {
        RateLimitProperties properties = new RateLimitProperties(true, null, null, 10_000);

        assertThat(properties.writes()).isEqualTo(new Limit(30, Duration.ofMinutes(1)));
        assertThat(properties.uploads()).isEqualTo(new Limit(10, Duration.ofMinutes(10)));
    }

    private RateLimitFilter filtro(int maxClaves) {
        return new RateLimitFilter(new RateLimitProperties(true, TRES_POR_MINUTO, UNA_CADA_DIEZ_MINUTOS, maxClaves),
                new ProblemDetailSecurityHandler(JsonMapper.builder().build()), reloj);
    }

    private MockHttpServletResponse enviar(String method, String uri) throws Exception {
        return enviar(new MockHttpServletRequest(method, uri));
    }

    /** Pasa la petición por los filtros indicados y después por el limitador; el controlador responde 200. */
    private MockHttpServletResponse enviar(MockHttpServletRequest request, Filter... antes) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        Filter[] filtros = Arrays.copyOf(antes, antes.length + 1);
        filtros[antes.length] = filter;
        new MockFilterChain(CONTROLADOR, filtros).doFilter(request, response);
        return response;
    }

    private static MockHttpServletRequest peticionDesde(String xForwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/resenas/1/reportes");
        request.setRemoteAddr("10.0.0.254");
        request.addHeader("X-Forwarded-For", xForwardedFor);
        return request;
    }

    private static void autenticar(String uid) {
        SecurityContextHolder.getContext().setAuthentication(new FirebaseAuthentication(
                new AuthenticatedUser(uid, uid + "@x.com", uid, null, true, Rol.USER)));
    }

    private void avanzar(Duration duracion) {
        nanos.addAndGet(duracion.toNanos());
    }
}
