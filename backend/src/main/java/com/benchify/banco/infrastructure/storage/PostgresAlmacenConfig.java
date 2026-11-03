package com.benchify.banco.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** {@code benchify.almacen.tipo=postgres}: fotos en la base de datos (despliegue gratuito). */
@Configuration
@ConditionalOnProperty(name = "benchify.almacen.tipo", havingValue = "postgres")
class PostgresAlmacenConfig {

    /**
     * @param urlPublicaApi dirección pública del backend, con la que se construyen las URLs de las
     *                      fotos aprobadas (en Render la da la plataforma en RENDER_EXTERNAL_URL)
     */
    @Bean
    PostgresAlmacenImagenes almacenImagenes(NamedParameterJdbcTemplate jdbc,
            @Value("${benchify.almacen.url-publica}") String urlPublicaApi) {
        return new PostgresAlmacenImagenes(jdbc, urlPublicaApi);
    }
}
