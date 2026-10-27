package com.benchify.shared.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    private final CorsProperties properties = new CorsProperties(
            List.of("http://localhost:4200"),
            List.of("GET", "POST"),
            List.of("Authorization"),
            List.of("Location"),
            Duration.ofMinutes(30));

    @Test
    void appliesConfiguredOriginsToApiRoutes() {
        CorsConfiguration config = new CorsConfig().corsConfigurationSource(properties)
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/bancos"));

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:4200");
        assertThat(config.getAllowedMethods()).containsExactly("GET", "POST");
        assertThat(config.getExposedHeaders()).containsExactly("Location");
        assertThat(config.getMaxAge()).isEqualTo(1800L);
        assertThat(config.getAllowCredentials()).isFalse();
    }

    @Test
    void doesNotApplyCorsOutsideTheApi() {
        CorsConfiguration config = new CorsConfig().corsConfigurationSource(properties)
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/actuator/health"));

        assertThat(config).isNull();
    }
}
