package com.benchify.shared.infrastructure.firebase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FirebasePropertiesTest {

    @Test
    void emulatorFlagsDependOnConfiguredHosts() {
        FirebaseProperties.Emulator none = new FirebaseProperties.Emulator(null, " ", null);
        assertThat(none.authEnabled()).isFalse();
        assertThat(none.storageEnabled()).isFalse();

        FirebaseProperties.Emulator local = new FirebaseProperties.Emulator("localhost:9099", "http://localhost:9199", "");
        assertThat(local.authEnabled()).isTrue();
        assertThat(local.publicStorageUrl()).isEqualTo("http://localhost:9199");
    }

    @Test
    void dockerSetupUsesTheBrowserFacingUrlForDownloads() {
        FirebaseProperties.Emulator docker =
                new FirebaseProperties.Emulator("firebase:9099", "http://firebase:9199", "http://localhost:9199");

        assertThat(docker.storageHost()).isEqualTo("http://firebase:9199");
        assertThat(docker.publicStorageUrl()).isEqualTo("http://localhost:9199");
    }
}
