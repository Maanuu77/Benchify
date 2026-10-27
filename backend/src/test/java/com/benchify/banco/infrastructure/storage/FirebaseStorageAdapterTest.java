package com.benchify.banco.infrastructure.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirebaseStorageAdapterTest {

    private final Storage storage = mock(Storage.class);
    private final FirebaseStorageAdapter adapter =
            new FirebaseStorageAdapter(storage, "benchify.appspot.com", "https://firebasestorage.googleapis.com/");

    @Test
    void uploadsWithContentTypeAndLongCacheAndReturnsDownloadUrl() throws Exception {
        byte[] bytes = {1, 2, 3};

        String url = adapter.guardar("bancos/abc/def.jpg", bytes, "image/jpeg");

        ArgumentCaptor<BlobInfo> blob = ArgumentCaptor.forClass(BlobInfo.class);
        ArgumentCaptor<InputStream> contenido = ArgumentCaptor.forClass(InputStream.class);
        verify(storage).createFrom(blob.capture(), contenido.capture());
        assertThat(contenido.getValue().readAllBytes()).isEqualTo(bytes);
        assertThat(blob.getValue().getBucket()).isEqualTo("benchify.appspot.com");
        assertThat(blob.getValue().getName()).isEqualTo("bancos/abc/def.jpg");
        assertThat(blob.getValue().getContentType()).isEqualTo("image/jpeg");
        assertThat(blob.getValue().getCacheControl()).isEqualTo(FirebaseStorageAdapter.CACHE_CONTROL);
        assertThat(url).isEqualTo(
                "https://firebasestorage.googleapis.com/v0/b/benchify.appspot.com/o/bancos%2Fabc%2Fdef.jpg?alt=media");
    }

    @Test
    void wrapsUploadErrors() throws Exception {
        when(storage.createFrom(any(BlobInfo.class), any(InputStream.class))).thenThrow(new IOException("broken pipe"));

        assertThatThrownBy(() -> adapter.guardar("bancos/x.jpg", new byte[]{1}, "image/jpeg"))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("bancos/x.jpg");
    }

    @Test
    void deletesByObjectPath() {
        adapter.borrar("bancos/abc/def.jpg");

        verify(storage).delete(BlobId.of("benchify.appspot.com", "bancos/abc/def.jpg"));
    }

    @Test
    void emulatorUrlsPointToTheEmulatorHost() {
        FirebaseStorageAdapter emulador = new FirebaseStorageAdapter(storage, "demo-benchify.appspot.com",
                "http://localhost:9199");

        assertThat(emulador.urlPublica("bancos/a b/c.png"))
                .isEqualTo("http://localhost:9199/v0/b/demo-benchify.appspot.com/o/bancos%2Fa%20b%2Fc.png?alt=media");
    }
}
