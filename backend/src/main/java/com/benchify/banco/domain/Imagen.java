package com.benchify.banco.domain;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Foto de un banco ya almacenada. Las de usuarios pasan por moderación; las de administradores no.
 * La miniatura ({@code rutaMiniatura}, {@code urlMiniatura}) falta en las fotos anteriores a que se generaran.
 */
public record Imagen(
        UUID id,
        UUID bancoId,
        String ruta,
        String url,
        String rutaMiniatura,
        String urlMiniatura,
        String contentType,
        long tamanoBytes,
        String subidaPor,
        EstadoImagen estado,
        Instant fechaSubida) {

    /** Máximo de fotos activas (pendientes o aprobadas) por banco. */
    public static final int MAX_POR_BANCO = 10;

    private static final String SUFIJO_MINIATURA = "_thumb";

    public Imagen {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bancoId, "bancoId");
        Objects.requireNonNull(ruta, "ruta");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(subidaPor, "subidaPor");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(fechaSubida, "fechaSubida");
        if ((rutaMiniatura == null) != (urlMiniatura == null)) {
            throw new IllegalArgumentException("La miniatura necesita ruta y URL");
        }
    }

    /** Foto sin miniatura (subida antes de que se generaran). */
    public Imagen(UUID id, UUID bancoId, String ruta, String url, String contentType, long tamanoBytes,
            String subidaPor, EstadoImagen estado, Instant fechaSubida) {
        this(id, bancoId, ruta, url, null, null, contentType, tamanoBytes, subidaPor, estado, fechaSubida);
    }

    /**
     * Ruta del objeto en el bucket: una carpeta por banco y un nombre imposible de adivinar.
     * Siempre {@code .jpg}: las fotos se recodifican a JPEG al subirlas.
     */
    public static String rutaPara(UUID bancoId, UUID imagenId) {
        return "bancos/" + bancoId + "/" + imagenId + ".jpg";
    }

    /** La miniatura vive junto a la foto: {@code <id>.jpg} → {@code <id>_thumb.jpg}. */
    public static String rutaMiniatura(String ruta) {
        int punto = ruta.lastIndexOf('.');
        return punto > ruta.lastIndexOf('/')
                ? ruta.substring(0, punto) + SUFIJO_MINIATURA + ruta.substring(punto)
                : ruta + SUFIJO_MINIATURA;
    }

    public static Imagen nueva(UUID id, UUID bancoId, FicheroAlmacenado foto, FicheroAlmacenado miniatura,
            long tamanoBytes, Actor actor, Instant ahora) {
        EstadoImagen estado = actor.isAdmin() ? EstadoImagen.APROBADA : EstadoImagen.PENDIENTE;
        return new Imagen(id, bancoId, foto.ruta(), foto.url(), miniatura.ruta(), miniatura.url(),
                ImagenProcesada.CONTENT_TYPE, tamanoBytes, actor.userId(), estado, ahora);
    }

    /** Aprobadas para todos; pendientes solo para quien la subió y administradores. */
    public boolean esVisiblePara(Actor actor) {
        return switch (estado) {
            case APROBADA -> true;
            case PENDIENTE -> actor != null && actor.canManage(subidaPor);
            case RECHAZADA -> false;
        };
    }

    /** Rutas de todos sus ficheros en el almacenamiento: la foto y, si la tiene, la miniatura. */
    public List<String> rutas() {
        return rutaMiniatura == null ? List.of(ruta) : List.of(ruta, rutaMiniatura);
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
        return new Imagen(id, bancoId, ruta, url, rutaMiniatura, urlMiniatura, contentType, tamanoBytes, subidaPor,
                nuevo, fechaSubida);
    }
}
