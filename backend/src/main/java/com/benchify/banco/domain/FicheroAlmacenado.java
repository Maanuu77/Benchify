package com.benchify.banco.domain;

import java.util.Objects;

/** Un fichero de una foto en el almacenamiento: su ruta en el bucket y la URL con la que se descarga. */
public record FicheroAlmacenado(String ruta, String url) {

    public FicheroAlmacenado {
        Objects.requireNonNull(ruta, "ruta");
        Objects.requireNonNull(url, "url");
    }
}
