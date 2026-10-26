package com.benchify.banco.application.port.out;

/** Almacenamiento de ficheros de imagen (Firebase Cloud Storage en producción). */
public interface AlmacenImagenesPort {

    /** Guarda el fichero en {@code ruta} y devuelve su URL pública de lectura. */
    String guardar(String ruta, byte[] contenido, String contentType);

    /** Borra el fichero; no falla si ya no existe. */
    void borrar(String ruta);
}
