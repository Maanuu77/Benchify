package com.benchify.banco.domain;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Foto de un banco ya almacenada. Las de usuarios pasan por moderación; las de administradores no. */
public record Imagen(
        UUID id,
        UUID bancoId,
        String ruta,
        String url,
        String contentType,
        long tamanoBytes,
        String subidaPor,
        EstadoImagen estado,
        Instant fechaSubida) {

    /** Máximo de fotos activas (pendientes o aprobadas) por banco. */
    public static final int MAX_POR_BANCO = 10;

    public Imagen {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bancoId, "bancoId");
        Objects.requireNonNull(ruta, "ruta");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(subidaPor, "subidaPor");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(fechaSubida, "fechaSubida");
    }

    /** Ruta del objeto en el bucket: una carpeta por banco y un nombre imposible de adivinar. */
    public static String rutaPara(UUID bancoId, UUID imagenId, ArchivoImagen archivo) {
        return "bancos/" + bancoId + "/" + imagenId + "." + archivo.extension();
    }

    public static Imagen nueva(UUID id, UUID bancoId, String ruta, String url, ArchivoImagen archivo, Actor actor,
            Instant ahora) {
        EstadoImagen estado = actor.isAdmin() ? EstadoImagen.APROBADA : EstadoImagen.PENDIENTE;
        return new Imagen(id, bancoId, ruta, url, archivo.contentType(), archivo.tamanoBytes(), actor.userId(),
                estado, ahora);
    }

    /** Aprobadas para todos; pendientes solo para quien la subió y administradores. */
    public boolean esVisiblePara(Actor actor) {
        return switch (estado) {
            case APROBADA -> true;
            case PENDIENTE -> actor != null && actor.canManage(subidaPor);
            case RECHAZADA -> false;
        };
    }

    public Imagen aprobar() {
        if (estado != EstadoImagen.PENDIENTE) {
            throw new BusinessRuleException("IMAGEN_NO_PENDIENTE", "Solo se pueden moderar imágenes pendientes");
        }
        return conEstado(EstadoImagen.APROBADA);
    }

    public Imagen rechazar() {
        if (estado != EstadoImagen.PENDIENTE) {
            throw new BusinessRuleException("IMAGEN_NO_PENDIENTE", "Solo se pueden moderar imágenes pendientes");
        }
        return conEstado(EstadoImagen.RECHAZADA);
    }

    private Imagen conEstado(EstadoImagen nuevo) {
        return new Imagen(id, bancoId, ruta, url, contentType, tamanoBytes, subidaPor, nuevo, fechaSubida);
    }
}
