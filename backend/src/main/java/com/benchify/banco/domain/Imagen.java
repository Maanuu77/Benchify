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

    /** Carpeta de las fotos aprobadas: cualquiera puede leer un objeto si conoce su nombre. */
    public static final String CARPETA_PUBLICA = "bancos/";

    /** Carpeta de las fotos pendientes de moderación: sin acceso público, se sirven a través de la API. */
    public static final String CARPETA_PENDIENTES = "pendientes/";

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

    /** Las fotos de usuarios esperan moderación; las de administradores se publican directamente. */
    public static EstadoImagen estadoInicial(Actor actor) {
        return actor.isAdmin() ? EstadoImagen.APROBADA : EstadoImagen.PENDIENTE;
    }

    /**
     * Ruta del objeto en el bucket: una carpeta por banco y un nombre imposible de adivinar. Las
     * aprobadas van a la carpeta pública ({@value #CARPETA_PUBLICA}) y las pendientes a una sin
     * acceso público ({@value #CARPETA_PENDIENTES}). Siempre {@code .jpg}: las fotos se recodifican.
     */
    public static String rutaPara(UUID bancoId, UUID imagenId, EstadoImagen estado) {
        String carpeta = estado == EstadoImagen.APROBADA ? CARPETA_PUBLICA : CARPETA_PENDIENTES;
        return carpeta + bancoId + "/" + imagenId + ".jpg";
    }

    /** La miniatura vive junto a la foto: {@code <id>.jpg} → {@code <id>_thumb.jpg}. */
    public static String rutaMiniatura(String ruta) {
        int punto = ruta.lastIndexOf('.');
        return punto > ruta.lastIndexOf('/')
                ? ruta.substring(0, punto) + SUFIJO_MINIATURA + ruta.substring(punto)
                : ruta + SUFIJO_MINIATURA;
    }

    /** Dónde queda un fichero al aprobar la foto: de la carpeta de pendientes pasa a la pública. */
    public static String rutaPublica(String ruta) {
        return ruta.startsWith(CARPETA_PENDIENTES)
                ? CARPETA_PUBLICA + ruta.substring(CARPETA_PENDIENTES.length())
                : ruta;
    }

    public static Imagen nueva(UUID id, UUID bancoId, FicheroAlmacenado foto, FicheroAlmacenado miniatura,
            long tamanoBytes, Actor actor, Instant ahora) {
        return new Imagen(id, bancoId, foto.ruta(), foto.url(), miniatura.ruta(), miniatura.url(),
                ImagenProcesada.CONTENT_TYPE, tamanoBytes, actor.userId(), estadoInicial(actor), ahora);
    }

    /**
     * Si sus ficheros están en la carpeta de pendientes y hay que moverlos al aprobarla. Las
     * pendientes subidas antes de existir esa carpeta siguen en la pública.
     */
    public boolean estaEnCarpetaPendientes() {
        return ruta.startsWith(CARPETA_PENDIENTES);
    }

    /** Fichero que se sirve: la miniatura o, si es anterior a las miniaturas, la foto completa. */
    public String rutaFichero(boolean miniatura) {
        return miniatura && rutaMiniatura != null ? rutaMiniatura : ruta;
    }

    /** La misma imagen con sus ficheros en otro sitio; {@code miniatura} es {@code null} si no tiene. */
    public Imagen conFicheros(FicheroAlmacenado foto, FicheroAlmacenado miniatura) {
        if ((miniatura == null) != (rutaMiniatura == null)) {
            throw new IllegalArgumentException("Se deben mover todos los ficheros de la imagen");
        }
        return new Imagen(id, bancoId, foto.ruta(), foto.url(), miniatura == null ? null : miniatura.ruta(),
                miniatura == null ? null : miniatura.url(), contentType, tamanoBytes, subidaPor, estado, fechaSubida);
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
        exigirPendiente();
        return conEstado(EstadoImagen.APROBADA);
    }

    public Imagen rechazar() {
        exigirPendiente();
        return conEstado(EstadoImagen.RECHAZADA);
    }

    public void exigirPendiente() {
        if (estado != EstadoImagen.PENDIENTE) {
            throw new BusinessRuleException("IMAGEN_NO_PENDIENTE", "Solo se pueden moderar imágenes pendientes");
        }
    }

    private Imagen conEstado(EstadoImagen nuevo) {
        return new Imagen(id, bancoId, ruta, url, rutaMiniatura, urlMiniatura, contentType, tamanoBytes, subidaPor,
                nuevo, fechaSubida);
    }
}
