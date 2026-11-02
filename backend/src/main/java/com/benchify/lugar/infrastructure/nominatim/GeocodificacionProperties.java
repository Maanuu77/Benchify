package com.benchify.lugar.infrastructure.nominatim;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * {@code benchify.geocodificacion.*}. La política de uso de Nominatim pide un User-Agent que
 * identifique la aplicación, como mucho una petición por segundo y guardar los resultados.
 *
 * @param url          raíz del servicio compatible con la API de Nominatim
 * @param intervalo    tiempo mínimo entre dos peticiones al servicio, sumando todos los usuarios
 * @param esperaMaxima lo que puede esperar una búsqueda su turno antes de responder 503
 * @param cache        cuánto se guardan los resultados de una búsqueda
 */
@ConfigurationProperties(prefix = "benchify.geocodificacion")
record GeocodificacionProperties(
        @DefaultValue("https://nominatim.openstreetmap.org") String url,
        @DefaultValue("Benchify/2.0 (+https://github.com/Maanuu77/Benchify)") String userAgent,
        @DefaultValue("4s") Duration timeout,
        @DefaultValue("1s") Duration intervalo,
        @DefaultValue("3s") Duration esperaMaxima,
        @DefaultValue("24h") Duration cache) {
}
