package com.benchify.banco.infrastructure.storage;

import com.benchify.banco.domain.Imagen;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;

/**
 * Hace de "bucket público" cuando las fotos se guardan en PostgreSQL: sirve solo la carpeta pública
 * (fotos aprobadas). Es parte del adaptador de almacenamiento, no de la API, igual que Cloud Storage
 * sirve sus propios ficheros. Cada ruta lleva el id de la foto y su contenido no cambia, así que el
 * navegador puede guardarla para siempre.
 */
@Configuration
@ConditionalOnProperty(name = "benchify.almacen.tipo", havingValue = "postgres")
class FicherosPublicosRouter {

    @Bean
    RouterFunction<ServerResponse> ficherosPublicos(PostgresAlmacenImagenes almacen) {
        return RouterFunctions.route()
                .GET(PostgresAlmacenImagenes.RUTA_PUBLICA + "{*ruta}",
                        peticion -> descargar(almacen, peticion.pathVariable("ruta")))
                .build();
    }

    private static ServerResponse descargar(PostgresAlmacenImagenes almacen, String ruta) {
        String limpia = ruta.startsWith("/") ? ruta.substring(1) : ruta;
        if (!limpia.startsWith(Imagen.CARPETA_PUBLICA) || limpia.contains("..")) {
            return ServerResponse.notFound().build();
        }
        return almacen.leer(limpia)
                .map(contenido -> ServerResponse.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .body(contenido))
                .orElseGet(() -> ServerResponse.notFound().build());
    }
}
