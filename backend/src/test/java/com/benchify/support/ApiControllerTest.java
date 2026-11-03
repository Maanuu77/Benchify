package com.benchify.support;

import com.benchify.shared.infrastructure.config.ClockConfig;
import com.benchify.shared.infrastructure.security.SecurityConfig;
import com.benchify.shared.infrastructure.web.CorsConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test de slice de un controlador con la cadena de seguridad real y el verificador de tokens
 * simulado ({@link StubIdTokenVerifier}); el rol es el del token ({@link TokenRoleResolver}).
 * Los casos de uso se simulan con {@code @MockitoBean}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebMvcTest
@ActiveProfiles("test")
@Import({SecurityConfig.class, CorsConfig.class, ClockConfig.class, TestAdaptersConfig.class, TokenRoleResolver.class})
public @interface ApiControllerTest {

    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};
}
