package com.benchify.shared.infrastructure.firebase;

import com.benchify.shared.infrastructure.security.IdTokenVerifier;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.internal.EmulatorCredentials;
import com.google.firebase.internal.FirebaseProcessEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@ConditionalOnBooleanProperty(name = "benchify.firebase.enabled", matchIfMissing = true)
public class FirebaseConfig {

    static final String APP_NAME = "benchify";
    static final String AUTH_EMULATOR_ENV = "FIREBASE_AUTH_EMULATOR_HOST";

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean(destroyMethod = "delete")
    FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        boolean emulator = properties.emulator().authEnabled();
        if (emulator && FirebaseProcessEnvironment.getenv(AUTH_EMULATOR_ENV) == null) {
            // El Admin SDK solo detecta el emulador por variable de entorno; la fijamos desde el perfil dev
            FirebaseProcessEnvironment.setenv(AUTH_EMULATOR_ENV, properties.emulator().authHost());
        }

        GoogleCredentials credentials = emulator
                ? new EmulatorCredentials()
                : GoogleCredentials.getApplicationDefault();

        FirebaseOptions.Builder options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(properties.projectId());
        if (properties.storageBucket() != null && !properties.storageBucket().isBlank()) {
            options.setStorageBucket(properties.storageBucket());
        }

        log.info("Initializing Firebase project '{}'{}", properties.projectId(),
                emulator ? " against the Auth emulator at " + properties.emulator().authHost() : "");
        return FirebaseApp.initializeApp(options.build(), APP_NAME);
    }

    @Bean
    FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    IdTokenVerifier firebaseIdTokenVerifier(FirebaseAuth firebaseAuth) {
        return new FirebaseIdTokenVerifier(firebaseAuth);
    }
}
