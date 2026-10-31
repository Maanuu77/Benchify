package com.benchify.banco.infrastructure.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
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
    void uploadsWithContentTypeAndADayOfCacheAndReturnsDownloadUrl() throws Exception {
        byte[] bytes = {1, 2, 3};

        String url = adapter.guardar("bancos/abc/def.jpg", bytes, "image/jpeg");

        ArgumentCaptor<BlobInfo> blob = ArgumentCaptor.forClass(BlobInfo.class);
        ArgumentCaptor<InputStream> contenido = ArgumentCaptor.forClass(InputStream.class);
        verify(storage).createFrom(blob.capture(), contenido.capture());
        assertThat(contenido.getValue().readAllBytes()).isEqualTo(bytes);
        assertThat(blob.getValue().getBucket()).isEqualTo("benchify.appspot.com");
        assertThat(blob.getValue().getName()).isEqualTo("bancos/abc/def.jpg");
        assertThat(blob.getValue().getContentType()).isEqualTo("image/jpeg");
        assertThat(blob.getValue().getCacheControl()).isEqualTo("public, max-age=86400");
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
    void readsPendingFilesWithTheAdminCredentials() {
        BlobId blob = BlobId.of("benchify.appspot.com", "pendientes/abc/def.jpg");
        when(storage.readAllBytes(blob)).thenReturn(new byte[]{4, 5});

        assertThat(adapter.leer("pendientes/abc/def.jpg")).hasValueSatisfying(
                contenido -> assertThat(contenido).containsExactly(4, 5));
    }

    @Test
    void aMissingFileReadsAsEmptyAndOtherErrorsPropagate() {
        when(storage.readAllBytes(BlobId.of("benchify.appspot.com", "pendientes/no-existe.jpg")))
                .thenThrow(new StorageException(404, "Not Found"));
        when(storage.readAllBytes(BlobId.of("benchify.appspot.com", "pendientes/caido.jpg")))
                .thenThrow(new StorageException(503, "Unavailable"));

        assertThat(adapter.leer("pendientes/no-existe.jpg")).isEmpty();
        assertThatThrownBy(() -> adapter.leer("pendientes/caido.jpg")).isInstanceOf(StorageException.class);
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
