package com.benchify.banco.infrastructure.procesado;

import com.benchify.banco.domain.ArchivoImagen;
import com.benchify.banco.domain.ImagenProcesada;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;

class HuellaPerceptualTest {

    private final ThumbnailatorProcesadorImagenesAdapter procesador = new ThumbnailatorProcesadorImagenesAdapter();

    @Test
    void theSamePhotoResizedAndRecompressedKeepsAlmostTheSameHash() {
        long original = huella(escena(1600, 1200, false), "png");
        long reducida = huella(escena(640, 480, false), "jpg");

        assertThat(distancia(original, reducida)).isLessThanOrEqualTo(ImagenProcesada.DISTANCIA_PARECIDAS);
    }

    @Test
    void differentPhotosAreFarApart() {
        long una = huella(escena(800, 600, false), "png");
        long volteada = huella(escena(800, 600, true), "png");

        assertThat(distancia(una, volteada)).isGreaterThan(ImagenProcesada.DISTANCIA_PARECIDAS * 3);
    }

    @Test
    void aFlatImageHasAnEmptyHash() {
        BufferedImage gris = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);

        assertThat(HuellaPerceptual.dhash(gris)).isZero();
    }

    private long huella(BufferedImage imagen, String formato) {
        String contentType = formato.equals("jpg") ? "image/jpeg" : "image/" + formato;
        return procesador.procesar(new ArchivoImagen(contentType, bytes(imagen, formato))).huella();
    }

    private static int distancia(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    /** Un degradado con unas cuantas formas: suficiente detalle para que el hash tenga bits de los dos valores. */
    private static BufferedImage escena(int ancho, int alto, boolean volteada) {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D lienzo = imagen.createGraphics();
        try {
            if (volteada) {
                lienzo.translate(ancho, 0);
                lienzo.scale(-1, 1);
            }
            lienzo.setPaint(new GradientPaint(0, 0, new Color(30, 120, 200), ancho, alto, new Color(240, 220, 120)));
            lienzo.fillRect(0, 0, ancho, alto);
            lienzo.setColor(new Color(40, 90, 30));
            lienzo.fillOval(ancho / 10, alto / 2, ancho / 3, alto / 3);
            lienzo.setColor(new Color(120, 70, 40));
            lienzo.fillRect(ancho / 2, alto / 5, ancho / 4, alto / 2);
            lienzo.setColor(Color.WHITE);
            lienzo.fillOval(ancho * 3 / 4, alto / 10, ancho / 8, ancho / 8);
        } finally {
            lienzo.dispose();
        }
        return imagen;
    }

    private static byte[] bytes(BufferedImage imagen, String formato) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            ImageIO.write(imagen, formato, salida);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return salida.toByteArray();
    }
}
