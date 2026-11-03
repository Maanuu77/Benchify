package com.benchify.banco.application.port.out;

import java.util.Optional;

/** Almacenamiento de ficheros de imagen (Firebase Cloud Storage en producción). */
public interface AlmacenImagenesPort {

    /**
     * Guarda el fichero en {@code ruta} y devuelve su URL de descarga. Solo funciona sin
     * autenticación en la carpeta pública: las reglas del bucket cierran el resto.
     */
    String guardar(String ruta, byte[] contenido, String contentType);

    /** Contenido del fichero, o vacío si no existe. */
    Optional<byte[]> leer(String ruta);

    /** Borra el fichero; no falla si ya no existe. */
    void borrar(String ruta);
}
