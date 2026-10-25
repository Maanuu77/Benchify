package com.benchify.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Contexto completo de Spring sobre PostgreSQL + PostGIS real. */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfTestDatabase
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        TestDatabase.Connection db = TestDatabase.connection();
        registry.add("spring.datasource.url", db::url);
        registry.add("spring.datasource.username", db::username);
        registry.add("spring.datasource.password", db::password);
    }
}
