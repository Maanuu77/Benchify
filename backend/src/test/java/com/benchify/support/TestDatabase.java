package com.benchify.support;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base de datos para los tests de integración.
 *
 * <p>Por defecto arranca un único contenedor PostGIS compartido por toda la ejecución.
 * Si no hay Docker (por ejemplo en un portátil sin Docker Desktop) se puede apuntar a un
 * PostgreSQL + PostGIS existente con {@code IT_DATABASE_URL}, {@code IT_DATABASE_USERNAME}
 * e {@code IT_DATABASE_PASSWORD}. Sin ninguna de las dos opciones los tests se omiten.
 */
public final class TestDatabase {

    static final DockerImageName POSTGIS_IMAGE = DockerImageName.parse("postgis/postgis:17-3.5")
            .asCompatibleSubstituteFor("postgres");

    private static PostgreSQLContainer container;

    private TestDatabase() {
    }

    public static boolean isAvailable() {
        return externalUrl() != null || isDockerAvailable();
    }

    static synchronized Connection connection() {
        String url = externalUrl();
        if (url != null) {
            return new Connection(url, env("IT_DATABASE_USERNAME", "benchify"), env("IT_DATABASE_PASSWORD", "benchify"));
        }
        if (container == null) {
            container = new PostgreSQLContainer(POSTGIS_IMAGE)
                    .withDatabaseName("benchify")
                    .withUsername("benchify")
                    .withPassword("benchify");
            container.start();
        }
        return new Connection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
    }

    private static String externalUrl() {
        String url = System.getenv("IT_DATABASE_URL");
        return url == null || url.isBlank() ? null : url;
    }

    private static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    record Connection(String url, String username, String password) {
    }
}
