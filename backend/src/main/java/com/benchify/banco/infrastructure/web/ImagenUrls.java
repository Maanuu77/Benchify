package com.benchify.banco.infrastructure.web;

import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * URLs con las que el cliente descarga una foto. Las aprobadas se leen directamente del almacenamiento;
 * las pendientes no son públicas allí, así que apuntan a la API, que las sirve a quien las puede ver.
 */
final class ImagenUrls {

    static final String RECURSO = "/api/v1/imagenes/{imagenId}";
    static final String ARCHIVO = RECURSO + "/archivo";
    static final String MINIATURA = RECURSO + "/miniatura";

    private ImagenUrls() {
    }

    /** Si hay que pedir la foto a la API con la cabecera {@code Authorization}. */
    static boolean requiereAutenticacion(Imagen imagen) {
        return imagen.estado() != EstadoImagen.APROBADA;
    }

    static String foto(Imagen imagen) {
        return requiereAutenticacion(imagen) ? api(ARCHIVO, imagen) : imagen.url();
    }

    /** Las fotos anteriores a las miniaturas usan la foto completa. */
    static String miniatura(Imagen imagen) {
        if (requiereAutenticacion(imagen)) {
            return api(MINIATURA, imagen);
        }
        return imagen.urlMiniatura() != null ? imagen.urlMiniatura() : imagen.url();
    }

    /** Absoluta, con el host por el que llegó la petición (respeta las cabeceras X-Forwarded-*). */
    static String api(String plantilla, Imagen imagen) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(plantilla)
                .buildAndExpand(imagen.id()).toUriString();
    }
}
