package com.benchify.support;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;

/** Imágenes reales generadas en memoria para los tests que las decodifican. */
public final class TestImages {

    /** WebP con pérdida (VP8) de 1×1 px: la imagen con la que los navegadores detectan el soporte de WebP. */
    public static final byte[] WEBP_VP8 = Base64.getDecoder()
            .decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");

    /** WebP sin pérdida (VP8L) de 1×1 px. */
    public static final byte[] WEBP_VP8L = Base64.getDecoder()
            .decode("UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==");

    private TestImages() {
    }

    /** JPEG con la mitad izquierda roja y la derecha azul, para comprobar giros. */
    public static byte[] jpeg(int ancho, int alto) {
        return escribir(mitades(ancho, alto, BufferedImage.TYPE_INT_RGB), "jpeg");
    }

    public static byte[] png(int ancho, int alto) {
        return escribir(mitades(ancho, alto, BufferedImage.TYPE_INT_RGB), "png");
    }

    /** PNG con canal alfa y todos sus píxeles transparentes. */
    public static byte[] pngTransparente(int ancho, int alto) {
        return escribir(new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB), "png");
    }

    public static BufferedImage leer(byte[] contenido) {
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(contenido));
            if (imagen == null) {
                throw new IllegalArgumentException("Not an image");
            }
            return imagen;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BufferedImage mitades(int ancho, int alto, int tipo) {
        BufferedImage imagen = new BufferedImage(ancho, alto, tipo);
        Graphics2D lienzo = imagen.createGraphics();
        lienzo.setColor(Color.RED);
        lienzo.fillRect(0, 0, ancho / 2, alto);
        lienzo.setColor(Color.BLUE);
        lienzo.fillRect(ancho / 2, 0, ancho - ancho / 2, alto);
        lienzo.dispose();
        return imagen;
    }

    private static byte[] escribir(BufferedImage imagen, String formato) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            ImageIO.write(imagen, formato, salida);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return salida.toByteArray();
    }
}
