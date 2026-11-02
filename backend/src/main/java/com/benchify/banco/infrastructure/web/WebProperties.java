package com.benchify.banco.infrastructure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code benchify.web.*}: dirección pública de la aplicación web, para los enlaces que se comparten,
 * el sitemap y las etiquetas Open Graph.
 *
 * @param url raíz sin barra final, p. ej. {@code https://benchify.web.app}
 */
@ConfigurationProperties(prefix = "benchify.web")
record WebProperties(String url) {

    WebProperties {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("benchify.web.url is required");
        }
        url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
