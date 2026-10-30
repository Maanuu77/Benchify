package com.benchify.banco.infrastructure.web;

import com.benchify.banco.application.port.in.ImagenesBancoUseCase;
import com.benchify.banco.domain.ArchivoImagen;
import com.benchify.banco.domain.EstadoImagen;
import com.benchify.banco.domain.Imagen;
import com.benchify.banco.domain.ImagenNotFoundException;
import com.benchify.shared.domain.Actor;
import com.benchify.support.ApiControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiControllerTest(ImagenController.class)
@Import(BancoWebMapperImpl.class)
class ImagenControllerTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 2};
    private static final UUID BANCO = UUID.randomUUID();
    private static final Imagen IMAGEN = new Imagen(UUID.randomUUID(), BANCO, "bancos/x.png",
            "https://storage/x.png", "image/png", PNG.length, "ana", EstadoImagen.PENDIENTE,
            Instant.parse("2026-10-25T10:00:00Z"));

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private ImagenesBancoUseCase imagenes;

    @Test
    void uploadsMultipartImage() {
        when(imagenes.subir(Actor.user("ana"), BANCO, new ArchivoImagen("image/png", PNG))).thenReturn(IMAGEN);

        assertThat(mvc.post().uri("/api/v1/bancos/{id}/imagenes", BANCO).header("Authorization", bearer("ana"))
                .multipart().file(new MockMultipartFile("archivo", "foto.png", "image/png", PNG)))
                .hasStatus(201)
                .hasHeader("Location", "https://storage/x.png")
                .bodyJson().extractingPath("$.estado").isEqualTo("PENDIENTE");
    }

    @Test
    void rejectsSpoofedContentWithoutCallingTheUseCase() {
        assertThat(mvc.post().uri("/api/v1/bancos/{id}/imagenes", BANCO).header("Authorization", bearer("ana"))
                .multipart().file(new MockMultipartFile("archivo", "foto.png", "image/png", "not an image".getBytes())))
                .hasStatus(422)
                .bodyJson().extractingPath("$.code").isEqualTo("IMAGEN_CONTENIDO_INVALIDO");
        verifyNoInteractions(imagenes);
    }

    @Test
    void rejectsOversizedFilesBeforeReadingThem() {
        byte[] enorme = new byte[(int) ArchivoImagen.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, enorme, 0, PNG.length);

        assertThat(mvc.post().uri("/api/v1/bancos/{id}/imagenes", BANCO).header("Authorization", bearer("ana"))
                .multipart().file(new MockMultipartFile("archivo", "foto.png", "image/png", enorme)))
                .hasStatus(422)
                .bodyJson().extractingPath("$.code").isEqualTo("IMAGEN_DEMASIADO_GRANDE");
        verifyNoInteractions(imagenes);
    }

    @Test
    void uploadRequiresAuthentication() {
        assertThat(mvc.post().uri("/api/v1/bancos/{id}/imagenes", BANCO)
                .multipart().file(new MockMultipartFile("archivo", "foto.png", "image/png", PNG)))
                .hasStatus(401);
    }

    @Test
    void missingFilePartIsBadRequest() {
        assertThat(mvc.post().uri("/api/v1/bancos/{id}/imagenes", BANCO).header("Authorization", bearer("ana"))
                .multipart().param("otro", "x"))
                .hasStatus(400);
    }

    @Test
    void listsImagesPubliclyWithTheirThumbnails() {
        Imagen antigua = new Imagen(UUID.randomUUID(), BANCO, "bancos/x.png", "https://storage/x.png", "image/png",
                PNG.length, "ana", EstadoImagen.APROBADA, Instant.parse("2026-10-25T10:00:00Z"));
        Imagen conMiniatura = new Imagen(UUID.randomUUID(), BANCO, "bancos/y.jpg", "https://storage/y.jpg",
                "bancos/y_thumb.jpg", "https://storage/y_thumb.jpg", "image/jpeg", 100, "ana", EstadoImagen.APROBADA,
                Instant.parse("2026-10-25T10:00:00Z"));
        when(imagenes.listar(BANCO, null)).thenReturn(List.of(antigua, conMiniatura));

        assertThat(mvc.get().uri("/api/v1/bancos/{id}/imagenes", BANCO))
                .hasStatusOk()
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$[0].url").isEqualTo("https://storage/x.png");
                    // Las fotos anteriores a las miniaturas usan la foto completa
                    assertThat(json).extractingPath("$[0].thumbUrl").isEqualTo("https://storage/x.png");
                    assertThat(json).extractingPath("$[0].requiereAutenticacion").isEqualTo(false);
                    assertThat(json).extractingPath("$[1].thumbUrl").isEqualTo("https://storage/y_thumb.jpg");
                });
    }

    @Test
    void pendingImagesPointToTheAuthenticatedApi() {
        when(imagenes.listar(BANCO, Actor.user("ana"))).thenReturn(List.of(IMAGEN));

        assertThat(mvc.get().uri("/api/v1/bancos/{id}/imagenes", BANCO).header("Authorization", bearer("ana")))
                .hasStatusOk()
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$[0].url")
                            .isEqualTo("http://localhost/api/v1/imagenes/" + IMAGEN.id() + "/archivo");
                    assertThat(json).extractingPath("$[0].thumbUrl")
                            .isEqualTo("http://localhost/api/v1/imagenes/" + IMAGEN.id() + "/miniatura");
                    assertThat(json).extractingPath("$[0].requiereAutenticacion").isEqualTo(true);
                });
    }

    @Test
    void servesImageFilesWithAPrivateShortCache() {
        when(imagenes.leerFichero(IMAGEN.id(), false, Actor.user("ana"))).thenReturn(new ArchivoImagen("image/png", PNG));
        when(imagenes.leerFichero(IMAGEN.id(), true, Actor.user("ana"))).thenReturn(new ArchivoImagen("image/jpeg", JPEG));

        assertThat(mvc.get().uri("/api/v1/imagenes/{id}/archivo", IMAGEN.id()).header("Authorization", bearer("ana")))
                .hasStatusOk()
                .hasContentType("image/png")
                .hasHeader("Cache-Control", "max-age=300, private")
                .body().isEqualTo(PNG);
        assertThat(mvc.get().uri("/api/v1/imagenes/{id}/miniatura", IMAGEN.id()).header("Authorization", bearer("ana")))
                .hasStatusOk()
                .hasContentType("image/jpeg")
                .body().isEqualTo(JPEG);
    }

    @Test
    void hiddenImageFilesAreNotFound() {
        when(imagenes.leerFichero(IMAGEN.id(), false, null)).thenThrow(new ImagenNotFoundException(IMAGEN.id()));

        assertThat(mvc.get().uri("/api/v1/imagenes/{id}/archivo", IMAGEN.id()))
                .hasStatus(404)
                .bodyJson().extractingPath("$.code").isEqualTo("IMAGEN_NOT_FOUND");
    }

    @Test
    void deletesImage() {
        assertThat(mvc.delete().uri("/api/v1/imagenes/{id}", IMAGEN.id()).header("Authorization", bearer("ana")))
                .hasStatus(204);
        verify(imagenes).eliminar(Actor.user("ana"), IMAGEN.id());
    }
}
