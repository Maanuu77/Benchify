package com.benchify.shared.infrastructure.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Límites de frecuencia de las escrituras ({@code POST}, {@code PUT}, {@code PATCH} y
 * {@code DELETE} bajo {@code /api/**}). Cada usuario, o cada IP si la petición es anónima, tiene
 * su propio cubo de tokens, que se rellena de forma continua.
 *
 * @param enabled {@code false} quita el filtro (los tests lo desactivan salvo los suyos)
 * @param writes  escrituras en general
 * @param uploads subidas de fotos, más estrictas porque ocupan almacenamiento y cola de moderación
 * @param maxKeys usuarios o IPs recordados a la vez; por encima se olvidan los menos recientes
 */
@ConfigurationProperties(prefix = "benchify.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        Limit writes,
        Limit uploads,
        @DefaultValue("10000") int maxKeys) {

    public static final Limit DEFAULT_WRITES = new Limit(30, Duration.ofMinutes(1));
    public static final Limit DEFAULT_UPLOADS = new Limit(10, Duration.ofMinutes(10));

    public RateLimitProperties {
        writes = writes != null ? writes : DEFAULT_WRITES;
        uploads = uploads != null ? uploads : DEFAULT_UPLOADS;
        if (maxKeys <= 0) {
            throw new IllegalArgumentException("benchify.rate-limit.max-keys must be positive");
        }
    }

    /**
     * @param capacity peticiones seguidas que admite el cubo lleno
     * @param period   tiempo en que se rellena entero desde vacío
     */
    public record Limit(long capacity, Duration period) {

        public Limit {
            if (capacity <= 0 || period == null || period.isNegative() || period.isZero()) {
                throw new IllegalArgumentException("A rate limit needs a positive capacity and period");
            }
        }
    }
}
