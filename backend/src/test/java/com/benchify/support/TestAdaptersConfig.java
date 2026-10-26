package com.benchify.support;

import com.benchify.shared.infrastructure.security.IdTokenVerifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Sustituye los adaptadores de Firebase (Auth, roles, Storage) por dobles en memoria. */
@TestConfiguration(proxyBeanMethods = false)
public class TestAdaptersConfig {

    @Bean
    IdTokenVerifier stubIdTokenVerifier() {
        return new StubIdTokenVerifier();
    }

    @Bean
    RecordingIdentityProvider recordingIdentityProvider() {
        return new RecordingIdentityProvider();
    }

    @Bean
    InMemoryImageStorage inMemoryImageStorage() {
        return new InMemoryImageStorage();
    }
}
