package com.benchify.shared.infrastructure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Orígenes que pueden llamar a la API desde el navegador. Se define por perfil:
 * localhost:4200 en desarrollo y el dominio de Firebase Hosting en producción. {@code ETag} e
 * {@code If-Match} permiten al cliente editar un banco sin pisar cambios ajenos.
 *
 * @param exposedHeaders cabeceras de la respuesta que puede leer la SPA, como {@code ETag} o el
 *                       {@code Retry-After} de un 429
 */
@ConfigurationProperties(prefix = "benchify.cors")
public record CorsProperties(
        @DefaultValue List<String> allowedOrigins,
        @DefaultValue({"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}) List<String> allowedMethods,
        @DefaultValue({"Authorization", "Content-Type", "Accept", "If-Match"}) List<String> allowedHeaders,
        @DefaultValue({"Location", "ETag", "Retry-After"}) List<String> exposedHeaders,
        @DefaultValue("1h") Duration maxAge) {
}
