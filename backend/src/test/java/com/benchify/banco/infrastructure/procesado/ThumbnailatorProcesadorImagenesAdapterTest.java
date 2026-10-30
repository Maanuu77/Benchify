package com.benchify.banco.infrastructure.procesado;

import com.benchify.banco.domain.ArchivoImagen;
import com.benchify.banco.domain.ImagenProcesada;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.support.TestImages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThumbnailatorProcesadorImagenesAdapterTest {

    private final ThumbnailatorProcesadorImagenesAdapter procesador = new ThumbnailatorProcesadorImagenesAdapter();

    @Test
    void reencodesAsJpegWithoutExifAndAppliesTheOrientation() {
        // Orientación 6: el móvil guardó la foto tumbada y pide girarla 90° en sentido horario
        byte[] original = conExif(TestImages.jpeg(200, 100), tiffConOrientacionYGps(6));
        assertThat(texto(original)).contains("Exif");

        ImagenProcesada procesada = procesar("image/jpeg", original);

        assertThat(texto(procesada.contenido())).doesNotContain("Exif");
        assertThat(texto(procesada.miniatura())).doesNotContain("Exif");
        BufferedImage foto = TestImages.leer(procesada.contenido());
        assertThat(foto.getWidth()).isEqualTo(100);
        assertThat(foto.getHeight()).isEqualTo(200);
        // La mitad izquierda (roja) del original queda arriba al girar en sentido horario
        assertThat(esParecido(foto.getRGB(50, 20), Color.RED)).isTrue();
        assertThat(esParecido(foto.getRGB(50, 180), Color.BLUE)).isTrue();
        assertThat(procesada.tamanoBytes()).isEqualTo(procesada.contenido().length);
    }

    @Test
    void limitsTheLongSideAndBuildsAThumbnail() {
        ImagenProcesada procesada = procesar("image/png", TestImages.png(3000, 1500));

        assertThat(dimensiones(procesada.contenido())).containsExactly(ImagenProcesada.LADO_MAXIMO, 1024);
        assertThat(dimensiones(procesada.miniatura())).containsExactly(ImagenProcesada.LADO_MAXIMO_MINIATURA, 240);
    }

    @Test
    void subsamplesHugeImagesWhileDecoding() {
        ImagenProcesada procesada = procesar("image/jpeg", TestImages.jpeg(4500, 90));

        assertThat(dimensiones(procesada.contenido())[0]).isEqualTo(ImagenProcesada.LADO_MAXIMO);
        assertThat(dimensiones(procesada.miniatura())[0]).isEqualTo(ImagenProcesada.LADO_MAXIMO_MINIATURA);
    }

    @Test
    void neverUpscalesSmallImages() {
        ImagenProcesada procesada = procesar("image/png", TestImages.png(300, 200));

        assertThat(dimensiones(procesada.contenido())).containsExactly(300, 200);
        assertThat(dimensiones(procesada.miniatura())).containsExactly(300, 200);
    }

    @Test
    void flattensTransparencyOntoWhite() {
        BufferedImage foto = TestImages.leer(procesar("image/png", TestImages.pngTransparente(20, 20)).contenido());

        assertThat(esParecido(foto.getRGB(10, 10), Color.WHITE)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("webps")
    void readsWebp(byte[] webp) {
        ImagenProcesada procesada = procesar("image/webp", webp);

        assertThat(procesada.contenido()).startsWith((byte) 0xFF, (byte) 0xD8, (byte) 0xFF);
        assertThat(dimensiones(procesada.contenido())).containsExactly(1, 1);
    }

    static Stream<byte[]> webps() {
        return Stream.of(TestImages.WEBP_VP8, TestImages.WEBP_VP8L);
    }

    @Test
    void rejectsImagesOverTheMegapixelLimitBeforeDecodingThem() {
        // La cabecera declara 8000×6000 px, pero los datos son de una imagen de 10×10: decodificarla fallaría
        byte[] bomba = conDimensionesPng(TestImages.png(10, 10), 8000, 6000);

        assertThatThrownBy(() -> procesar("image/png", bomba))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("IMAGEN_RESOLUCION_EXCESIVA");
    }

    @Test
    void rejectsContentThatCannotBeDecoded() {
        byte[] truncado = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};

        assertThatThrownBy(() -> procesar("image/jpeg", truncado))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("IMAGEN_CONTENIDO_INVALIDO");
    }

    @Test
    void aDamagedExifBlockDoesNotInvalidateThePhoto() {
        byte[] original = conExif(TestImages.jpeg(40, 20), new byte[]{'M', 'M', 0, 42, 0, 0, 0, 99});

        assertThat(dimensiones(procesar("image/jpeg", original).contenido())).containsExactly(40, 20);
    }

    private ImagenProcesada procesar(String contentType, byte[] contenido) {
        return procesador.procesar(new ArchivoImagen(contentType, contenido));
    }

    private static int[] dimensiones(byte[] jpeg) {
        BufferedImage imagen = TestImages.leer(jpeg);
        return new int[]{imagen.getWidth(), imagen.getHeight()};
    }

    private static boolean esParecido(int rgb, Color esperado) {
        Color color = new Color(rgb);
        // JPEG tiene pérdidas: basta con que cada canal se acerque
        return Math.abs(color.getRed() - esperado.getRed()) < 40
                && Math.abs(color.getGreen() - esperado.getGreen()) < 40
                && Math.abs(color.getBlue() - esperado.getBlue()) < 40;
    }

    private static String texto(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    /** Inserta un segmento APP1 "Exif" tras la cabecera JFIF que escribe ImageIO. */
    private static byte[] conExif(byte[] jpeg, byte[] tiff) {
        int finApp0 = 4 + (((jpeg[4] & 0xFF) << 8) | (jpeg[5] & 0xFF));
        byte[] cabecera = "Exif\0\0".getBytes(StandardCharsets.ISO_8859_1);
        int longitud = 2 + cabecera.length + tiff.length;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write(jpeg, 0, finApp0);
        salida.write(0xFF);
        salida.write(0xE1);
        salida.write(longitud >> 8);
        salida.write(longitud & 0xFF);
        salida.writeBytes(cabecera);
        salida.writeBytes(tiff);
        salida.write(jpeg, finApp0, jpeg.length - finApp0);
        return salida.toByteArray();
    }

    /** TIFF little-endian con la orientación y un bloque GPS (latitud norte), como el de un móvil. */
    private static byte[] tiffConOrientacionYGps(int orientacion) {
        ByteBuffer tiff = ByteBuffer.allocate(56).order(ByteOrder.LITTLE_ENDIAN);
        tiff.put((byte) 'I').put((byte) 'I').putShort((short) 42).putInt(8);
        // IFD0 (offset 8): orientación y puntero al IFD de GPS
        tiff.putShort((short) 2);
        tiff.putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientacion).putShort((short) 0);
        tiff.putShort((short) 0x8825).putShort((short) 4).putInt(1).putInt(38);
        tiff.putInt(0);
        // IFD de GPS (offset 38): GPSLatitudeRef = "N"
        tiff.putShort((short) 1);
        tiff.putShort((short) 0x0001).putShort((short) 2).putInt(2).put((byte) 'N').put((byte) 0).putShort((short) 0);
        tiff.putInt(0);
        return tiff.array();
    }

    /** Cambia el ancho y el alto de la cabecera IHDR (y su CRC) sin tocar los datos de la imagen. */
    private static byte[] conDimensionesPng(byte[] png, int ancho, int alto) {
        ByteBuffer buffer = ByteBuffer.wrap(png);
        buffer.putInt(16, ancho).putInt(20, alto);
        CRC32 crc = new CRC32();
        crc.update(png, 12, 17);
        buffer.putInt(29, (int) crc.getValue());
        return png;
    }
}
