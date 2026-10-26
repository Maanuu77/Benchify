package com.benchify.banco.domain;

import com.benchify.shared.domain.BusinessRuleException;

import java.util.Arrays;
import java.util.Map;

/**
 * Fichero subido por un usuario. Comprueba el tamaño y que los primeros bytes correspondan de
 * verdad al formato declarado, para que no se pueda colar otro tipo de fichero renombrado.
 */
public record ArchivoImagen(String contentType, byte[] contenido) {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> EXTENSIONES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    public ArchivoImagen {
        if (contenido == null || contenido.length == 0) {
            throw new BusinessRuleException("IMAGEN_VACIA", "El fichero está vacío");
        }
        if (contenido.length > MAX_BYTES) {
            throw new BusinessRuleException("IMAGEN_DEMASIADO_GRANDE", "La imagen no puede superar 5 MB");
        }
        if (contentType == null || !EXTENSIONES.containsKey(contentType)) {
            throw new BusinessRuleException("IMAGEN_FORMATO_NO_SOPORTADO", "Formatos admitidos: JPEG, PNG y WebP");
        }
        if (!firmaCoincide(contentType, contenido)) {
            throw new BusinessRuleException("IMAGEN_CONTENIDO_INVALIDO",
                    "El contenido del fichero no corresponde a una imagen " + EXTENSIONES.get(contentType).toUpperCase());
        }
    }

    public String extension() {
        return EXTENSIONES.get(contentType);
    }

    public long tamanoBytes() {
        return contenido.length;
    }

    private static boolean firmaCoincide(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg" -> empiezaPor(bytes, 0, 0xFF, 0xD8, 0xFF);
            case "image/png" -> empiezaPor(bytes, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/webp" -> empiezaPor(bytes, 0, 'R', 'I', 'F', 'F') && empiezaPor(bytes, 8, 'W', 'E', 'B', 'P');
            default -> false;
        };
    }

    private static boolean empiezaPor(byte[] bytes, int offset, int... firma) {
        if (bytes.length < offset + firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((bytes[offset + i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }

    // Los arrays no tienen igualdad por valor: se redefine para que el record sea comparable
    @Override
    public boolean equals(Object o) {
        return o instanceof ArchivoImagen other
                && contentType.equals(other.contentType) && Arrays.equals(contenido, other.contenido);
    }

    @Override
    public int hashCode() {
        return 31 * contentType.hashCode() + Arrays.hashCode(contenido);
    }

    @Override
    public String toString() {
        return "ArchivoImagen[" + contentType + ", " + contenido.length + " bytes]";
    }
}
