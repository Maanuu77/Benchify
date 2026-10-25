package com.benchify.support;

import com.benchify.shared.infrastructure.security.IdTokenVerifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfig {

    @Bean
    IdTokenVerifier stubIdTokenVerifier() {
        return new StubIdTokenVerifier();
    }
}
