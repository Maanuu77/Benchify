package com.benchify.shared.infrastructure.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de Firebase. Las credenciales nunca se configuran aquí: en producción se usan
 * las Application Default Credentials (cuenta de servicio de Cloud Run o
 * {@code GOOGLE_APPLICATION_CREDENTIALS}); con emuladores no hacen falta.
 *
 * @param enabled       {@code false} en tests, donde un verificador de tokens simulado sustituye a Firebase
 * @param projectId     ID del proyecto de Firebase
 * @param storageBucket bucket de Cloud Storage para las imágenes de bancos
 * @param emulator      hosts de Firebase Emulator Suite (vacíos fuera de desarrollo)
 */
@ConfigurationProperties(prefix = "benchify.firebase")
public record FirebaseProperties(
        @DefaultValue("true") boolean enabled,
        String projectId,
        String storageBucket,
        @DefaultValue Emulator emulator) {

    /**
     * @param authHost         host:puerto del emulador de Authentication, p. ej. {@code localhost:9099}
     * @param storageHost      URL del emulador de Storage que usa el backend, p. ej. {@code http://localhost:9199}
     * @param storagePublicUrl URL del emulador vista desde el navegador; por defecto, {@code storageHost}.
     *                         Difiere en Docker, donde el backend lo llama por el nombre del servicio.
     */
    public record Emulator(String authHost, String storageHost, String storagePublicUrl) {

        public boolean authEnabled() {
            return authHost != null && !authHost.isBlank();
        }

        public boolean storageEnabled() {
            return storageHost != null && !storageHost.isBlank();
        }

        public String publicStorageUrl() {
            return storagePublicUrl != null && !storagePublicUrl.isBlank() ? storagePublicUrl : storageHost;
        }
    }
}
