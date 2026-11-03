package com.benchify.shared.infrastructure.ratelimit;

import com.benchify.shared.infrastructure.security.ProblemDetailSecurityHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnBooleanProperty(name = "benchify.rate-limit.enabled", matchIfMissing = true)
public class RateLimitConfig {

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimitProperties properties,
            ProblemDetailSecurityHandler problemHandler) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(properties, problemHandler));
        registration.addUrlPatterns("/api/*");
        // Justo después de la cadena de Spring Security: el usuario ya está autenticado y las
        // peticiones rechazadas por ella (sin token, sin permisos) no gastan cupo
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 1);
        return registration;
    }
}
