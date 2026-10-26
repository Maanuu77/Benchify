package com.benchify.banco.infrastructure.storage;

import com.benchify.banco.application.port.out.AlmacenImagenesPort;
import com.benchify.shared.infrastructure.firebase.FirebaseProperties;
import com.google.cloud.NoCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBooleanProperty(name = "benchify.firebase.enabled", matchIfMissing = true)
class FirebaseStorageConfig {

    static final String FIREBASE_DOWNLOAD_HOST = "https://firebasestorage.googleapis.com";

    @Bean
    Storage firebaseStorage(FirebaseApp firebaseApp, FirebaseProperties properties) {
        if (properties.emulator().storageEnabled()) {
            // El emulador de Storage habla la API JSON de GCS y no comprueba credenciales
            return StorageOptions.newBuilder()
                    .setHost(properties.emulator().storageHost())
                    .setProjectId(properties.projectId())
                    .setCredentials(NoCredentials.getInstance())
                    .build()
                    .getService();
        }
        return StorageClient.getInstance(firebaseApp).bucket().getStorage();
    }

    @Bean
    AlmacenImagenesPort almacenImagenes(Storage firebaseStorage, FirebaseProperties properties) {
        String publicBaseUrl = properties.emulator().storageEnabled()
                ? properties.emulator().publicStorageUrl()
                : FIREBASE_DOWNLOAD_HOST;
        return new FirebaseStorageAdapter(firebaseStorage, properties.storageBucket(), publicBaseUrl);
    }
}
