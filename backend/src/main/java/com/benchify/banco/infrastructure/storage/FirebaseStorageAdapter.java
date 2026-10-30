package com.benchify.banco.infrastructure.storage;

import com.benchify.banco.application.port.out.AlmacenImagenesPort;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Sube las fotos al bucket de Firebase Cloud Storage con el Admin SDK (que ignora las reglas
 * de seguridad) y devuelve su URL de descarga. Las reglas del bucket solo dejan leer, uno a uno,
 * los objetos de la carpeta pública: el resto (fotos pendientes) solo lo lee el backend.
 */
public class FirebaseStorageAdapter implements AlmacenImagenesPort {

    /**
     * Un día de caché: una foto rechazada o borrada deja de servirse desde el navegador o la CDN
     * como mucho al día siguiente.
     */
    static final String CACHE_CONTROL = "public, max-age=86400";

    private static final int NO_ENCONTRADO = 404;

    private final Storage storage;
    private final String bucket;
    private final String publicBaseUrl;

    public FirebaseStorageAdapter(Storage storage, String bucket, String publicBaseUrl) {
        this.storage = storage;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @Override
    public String guardar(String ruta, byte[] contenido, String contentType) {
        BlobInfo blob = BlobInfo.newBuilder(BlobId.of(bucket, ruta))
                .setContentType(contentType)
                .setCacheControl(CACHE_CONTROL)
                .build();
        try {
            // Subida resumable: funciona igual en GCS y en el emulador, que no admite el multipart del cliente Java
            storage.createFrom(blob, new ByteArrayInputStream(contenido));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not upload " + ruta, e);
        }
        return urlPublica(ruta);
    }

    @Override
    public Optional<byte[]> leer(String ruta) {
        try {
            return Optional.of(storage.readAllBytes(BlobId.of(bucket, ruta)));
        } catch (StorageException e) {
            if (e.getCode() == NO_ENCONTRADO) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public void borrar(String ruta) {
        storage.delete(BlobId.of(bucket, ruta));
    }

    /** Formato de descarga de Firebase: la ruta completa va codificada como un único segmento. */
    String urlPublica(String ruta) {
        return publicBaseUrl + "/v0/b/" + bucket + "/o/" + UriUtils.encode(ruta, StandardCharsets.UTF_8) + "?alt=media";
    }
}
