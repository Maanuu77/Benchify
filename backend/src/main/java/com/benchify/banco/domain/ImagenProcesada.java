package com.benchify.banco.domain;

import java.util.Arrays;
import java.util.Objects;

/**
 * Foto lista para guardarse: recodificada a JPEG sin metadatos (EXIF, GPS…), con la orientación
 * aplicada y el lado mayor limitado, más su miniatura para galerías y listados.
 *
 * @param huella huella perceptual de 64 bits: dos fotos casi iguales difieren en pocos bits
 */
public record ImagenProcesada(byte[] contenido, byte[] miniatura, long huella) {

    /** Bits distintos entre dos huellas a partir de los cuales las fotos ya no se consideran parecidas. */
    public static final int DISTANCIA_PARECIDAS = 6;

    /** Todas las fotos se guardan como JPEG, sea cual sea el formato subido. */
    public static final String CONTENT_TYPE = "image/jpeg";

    /** Lado mayor de la foto publicada, en píxeles: las más grandes se reducen. */
    public static final int LADO_MAXIMO = 2048;

    /** Lado mayor de la miniatura (tarjetas de la galería y cola de moderación). */
    public static final int LADO_MAXIMO_MINIATURA = 480;

    /**
     * Resolución máxima que se llega a decodificar. Un fichero pequeño puede declarar dimensiones
     * enormes y agotar la memoria al descomprimirlo (bomba de descompresión).
     */
    public static final long MAX_PIXELES = 40_000_000L;

    public ImagenProcesada {
        Objects.requireNonNull(contenido, "contenido");
        Objects.requireNonNull(miniatura, "miniatura");
    }

    public long tamanoBytes() {
        return contenido.length;
    }

    // Los arrays no tienen igualdad por valor: se redefine para que el record sea comparable
    @Override
    public boolean equals(Object o) {
        return o instanceof ImagenProcesada other && huella == other.huella
                && Arrays.equals(contenido, other.contenido) && Arrays.equals(miniatura, other.miniatura);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Arrays.hashCode(contenido) + Arrays.hashCode(miniatura)) + Long.hashCode(huella);
    }

    @Override
    public String toString() {
        return "ImagenProcesada[" + contenido.length + " bytes, miniatura " + miniatura.length + " bytes, huella "
                + Long.toHexString(huella) + "]";
    }
}
