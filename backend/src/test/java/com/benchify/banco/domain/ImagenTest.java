package com.benchify.banco.domain;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagenTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P'};

    private static final UUID BANCO = UUID.randomUUID();
    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");

    @Test
    void acceptsRealJpegPngAndWebp() {
        assertThat(new ArchivoImagen("image/jpeg", JPEG).extension()).isEqualTo("jpg");
        assertThat(new ArchivoImagen("image/png", PNG).extension()).isEqualTo("png");
        assertThat(new ArchivoImagen("image/webp", WEBP).extension()).isEqualTo("webp");
    }

    @Test
    void rejectsFilesWhoseBytesDoNotMatchTheDeclaredType() {
        assertThatThrownBy(() -> new ArchivoImagen("image/jpeg", PNG))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("IMAGEN_CONTENIDO_INVALIDO");
        assertThatThrownBy(() -> new ArchivoImagen("image/png", "<?php echo 1;".getBytes()))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new ArchivoImagen("image/webp", new byte[]{'R', 'I', 'F', 'F'}))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsUnsupportedEmptyOrOversizedFiles() {
        assertThatThrownBy(() -> new ArchivoImagen("image/gif", "GIF89a".getBytes()))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("IMAGEN_FORMATO_NO_SOPORTADO");
        assertThatThrownBy(() -> new ArchivoImagen("image/jpeg", new byte[0]))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("IMAGEN_VACIA");
        byte[] enorme = new byte[(int) ArchivoImagen.MAX_BYTES + 1];
        System.arraycopy(JPEG, 0, enorme, 0, JPEG.length);
        assertThatThrownBy(() -> new ArchivoImagen("image/jpeg", enorme))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("IMAGEN_DEMASIADO_GRANDE");
    }

    @Test
    void archivoHasValueEquality() {
        assertThat(new ArchivoImagen("image/jpeg", JPEG.clone())).isEqualTo(new ArchivoImagen("image/jpeg", JPEG))
                .hasSameHashCodeAs(new ArchivoImagen("image/jpeg", JPEG));
        assertThat(new ArchivoImagen("image/jpeg", JPEG)).hasToString("ArchivoImagen[image/jpeg, 6 bytes]");
    }

    @Test
    void sizeCanBeCheckedBeforeReadingTheFile() {
        ArchivoImagen.comprobarTamano(ArchivoImagen.MAX_BYTES);

        assertThatThrownBy(() -> ArchivoImagen.comprobarTamano(ArchivoImagen.MAX_BYTES + 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("10 MB")
                .extracting("code").isEqualTo("IMAGEN_DEMASIADO_GRANDE");
    }

    @Test
    void storagePathGroupsImagesByBenchAndTheThumbnailLivesNextToThePhoto() {
        UUID imagen = UUID.randomUUID();
        String ruta = Imagen.rutaPara(BANCO, imagen, EstadoImagen.APROBADA);

        assertThat(ruta).isEqualTo("bancos/" + BANCO + "/" + imagen + ".jpg");
        assertThat(Imagen.rutaMiniatura(ruta)).isEqualTo("bancos/" + BANCO + "/" + imagen + "_thumb.jpg");
        assertThat(Imagen.rutaMiniatura("bancos/sin.punto/foto")).isEqualTo("bancos/sin.punto/foto_thumb");
    }

    @Test
    void pendingPhotosLiveInAPrivateFolderUntilTheyArePublished() {
        UUID imagen = UUID.randomUUID();
        String ruta = Imagen.rutaPara(BANCO, imagen, EstadoImagen.PENDIENTE);

        assertThat(ruta).isEqualTo("pendientes/" + BANCO + "/" + imagen + ".jpg");
        assertThat(Imagen.rutaPublica(ruta)).isEqualTo("bancos/" + BANCO + "/" + imagen + ".jpg");
        assertThat(Imagen.rutaPublica(Imagen.rutaMiniatura(ruta)))
                .isEqualTo("bancos/" + BANCO + "/" + imagen + "_thumb.jpg");
        // Las pendientes subidas antes de la carpeta privada ya están en la pública
        assertThat(Imagen.rutaPublica("bancos/x/y.png")).isEqualTo("bancos/x/y.png");
        assertThat(Imagen.estadoInicial(Actor.user("ana"))).isEqualTo(EstadoImagen.PENDIENTE);
        assertThat(Imagen.estadoInicial(Actor.admin("root"))).isEqualTo(EstadoImagen.APROBADA);
    }

    @Test
    void publishingMovesEveryFileOfThePhoto() {
        Imagen pendiente = nueva(Actor.user("ana"));
        String publica = Imagen.rutaPublica(pendiente.ruta());
        String miniaturaPublica = Imagen.rutaPublica(pendiente.rutaMiniatura());

        Imagen aprobada = pendiente.aprobar().conFicheros(new FicheroAlmacenado(publica, "https://cdn/foto"),
                new FicheroAlmacenado(miniaturaPublica, "https://cdn/mini"));

        assertThat(pendiente.estaEnCarpetaPendientes()).isTrue();
        assertThat(aprobada.estaEnCarpetaPendientes()).isFalse();
        assertThat(aprobada.rutas()).containsExactly(publica, miniaturaPublica);
        assertThat(aprobada.url()).isEqualTo("https://cdn/foto");
        assertThat(aprobada.urlMiniatura()).isEqualTo("https://cdn/mini");
        assertThat(aprobada).usingRecursiveComparison()
                .ignoringFields("ruta", "url", "rutaMiniatura", "urlMiniatura", "estado")
                .isEqualTo(pendiente);
        assertThatThrownBy(() -> pendiente.conFicheros(new FicheroAlmacenado(publica, "https://cdn/foto"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void servesTheThumbnailOrTheFullPhotoForLegacyRows() {
        Imagen conMiniatura = nueva(Actor.user("ana"));
        Imagen antigua = new Imagen(UUID.randomUUID(), BANCO, "bancos/x.png", "https://cdn/x.png", "image/png", 10,
                "ana", EstadoImagen.PENDIENTE, AHORA);

        assertThat(conMiniatura.rutaFichero(true)).isEqualTo(conMiniatura.rutaMiniatura());
        assertThat(conMiniatura.rutaFichero(false)).isEqualTo(conMiniatura.ruta());
        assertThat(antigua.rutaFichero(true)).isEqualTo("bancos/x.png");
        assertThat(antigua.conFicheros(new FicheroAlmacenado("bancos/z.png", "https://cdn/z.png"), null).rutas())
                .containsExactly("bancos/z.png");
    }

    @Test
    void userUploadsArePendingAndOnlyVisibleToUploaderAndAdmins() {
        Imagen imagen = nueva(Actor.user("ana"));

        assertThat(imagen.estado()).isEqualTo(EstadoImagen.PENDIENTE);
        assertThat(imagen.contentType()).isEqualTo("image/jpeg");
        assertThat(imagen.tamanoBytes()).isEqualTo(1234);
        assertThat(imagen.rutas()).containsExactly(imagen.ruta(), imagen.rutaMiniatura());
        assertThat(imagen.esVisiblePara(null)).isFalse();
        assertThat(imagen.esVisiblePara(Actor.user("luis"))).isFalse();
        assertThat(imagen.esVisiblePara(Actor.user("ana"))).isTrue();
        assertThat(imagen.esVisiblePara(Actor.admin("root"))).isTrue();
    }

    @Test
    void adminUploadsAreApprovedDirectly() {
        assertThat(nueva(Actor.admin("root")).estado()).isEqualTo(EstadoImagen.APROBADA);
    }

    @Test
    void moderationOnlyAppliesToPendingImages() {
        Imagen aprobada = nueva(Actor.user("ana")).aprobar();
        assertThat(aprobada.estado()).isEqualTo(EstadoImagen.APROBADA);
        assertThat(aprobada.esVisiblePara(null)).isTrue();

        Imagen rechazada = nueva(Actor.user("ana")).rechazar();
        assertThat(rechazada.esVisiblePara(Actor.admin("root"))).isFalse();

        assertThatThrownBy(aprobada::rechazar).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(rechazada::aprobar).isInstanceOf(BusinessRuleException.class);
        assertThat(rechazada.rutaMiniatura()).isNotNull();
    }

    @Test
    void imagesUploadedBeforeThumbnailsOnlyHaveOneFile() {
        Imagen antigua = new Imagen(UUID.randomUUID(), BANCO, "bancos/x.png", "https://cdn/x.png", "image/png", 10,
                "ana", EstadoImagen.APROBADA, AHORA);

        assertThat(antigua.rutaMiniatura()).isNull();
        assertThat(antigua.urlMiniatura()).isNull();
        assertThat(antigua.rutas()).containsExactly("bancos/x.png");
    }

    @Test
    void aThumbnailNeedsBothPathAndUrl() {
        assertThatThrownBy(() -> new Imagen(UUID.randomUUID(), BANCO, "bancos/x.jpg", "https://cdn/x.jpg",
                "bancos/x_thumb.jpg", null, "image/jpeg", 10, "ana", EstadoImagen.APROBADA, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processedImageHasValueEquality() {
        ImagenProcesada procesada = new ImagenProcesada(JPEG.clone(), PNG.clone());

        assertThat(procesada).isEqualTo(new ImagenProcesada(JPEG, PNG)).hasSameHashCodeAs(new ImagenProcesada(JPEG, PNG));
        assertThat(procesada).isNotEqualTo(new ImagenProcesada(JPEG, JPEG));
        assertThat(procesada.tamanoBytes()).isEqualTo(JPEG.length);
        assertThat(procesada).hasToString("ImagenProcesada[6 bytes, miniatura 10 bytes]");
    }

    private static Imagen nueva(Actor actor) {
        UUID id = UUID.randomUUID();
        String ruta = Imagen.rutaPara(BANCO, id, Imagen.estadoInicial(actor));
        return Imagen.nueva(id, BANCO, new FicheroAlmacenado(ruta, "https://cdn/" + ruta),
                new FicheroAlmacenado(Imagen.rutaMiniatura(ruta), "https://cdn/thumb"), 1234, actor, AHORA);
    }
}
