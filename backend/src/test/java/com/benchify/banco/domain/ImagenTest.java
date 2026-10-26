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
    void storagePathGroupsImagesByBench() {
        UUID imagen = UUID.randomUUID();

        assertThat(Imagen.rutaPara(BANCO, imagen, new ArchivoImagen("image/png", PNG)))
                .isEqualTo("bancos/" + BANCO + "/" + imagen + ".png");
    }

    @Test
    void userUploadsArePendingAndOnlyVisibleToUploaderAndAdmins() {
        Imagen imagen = nueva(Actor.user("ana"));

        assertThat(imagen.estado()).isEqualTo(EstadoImagen.PENDIENTE);
        assertThat(imagen.tamanoBytes()).isEqualTo(JPEG.length);
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
    }

    private static Imagen nueva(Actor actor) {
        UUID id = UUID.randomUUID();
        ArchivoImagen archivo = new ArchivoImagen("image/jpeg", JPEG);
        return Imagen.nueva(id, BANCO, Imagen.rutaPara(BANCO, id, archivo), "https://cdn/x.jpg", archivo, actor, AHORA);
    }
}
