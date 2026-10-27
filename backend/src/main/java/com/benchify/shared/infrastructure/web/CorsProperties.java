package com.benchify.shared.infrastructure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Orígenes que pueden llamar a la API desde el navegador. Se define por perfil:
 * localhost:4200 en desarrollo y el dominio de Firebase Hosting en producción.
 */
@ConfigurationProperties(prefix = "benchify.cors")
public record CorsProperties(
        @DefaultValue List<String> allowedOrigins,
        @DefaultValue({"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}) List<String> allowedMethods,
        @DefaultValue({"Authorization", "Content-Type", "Accept"}) List<String> allowedHeaders,
        @DefaultValue({"Location"}) List<String> exposedHeaders,
        @DefaultValue("1h") Duration maxAge) {
}
