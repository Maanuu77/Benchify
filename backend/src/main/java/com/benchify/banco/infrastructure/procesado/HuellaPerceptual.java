package com.benchify.banco.infrastructure.procesado;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * dHash ("difference hash"): la foto se reduce a 9×8 píxeles en gris y cada bit dice si un píxel
 * es más claro que el de su derecha. Recomprimir, reescalar o retocar un poco el brillo apenas
 * cambia los bits, así que dos fotos casi iguales quedan a poca distancia de Hamming.
 */
final class HuellaPerceptual {

    private static final int ANCHO = 9;
    private static final int ALTO = 8;

    private HuellaPerceptual() {
    }

    static long dhash(BufferedImage imagen) {
        BufferedImage gris = reducirEnGris(imagen);
        long huella = 0;
        for (int y = 0; y < ALTO; y++) {
            for (int x = 0; x < ANCHO - 1; x++) {
                huella <<= 1;
                if (brillo(gris, x, y) > brillo(gris, x + 1, y)) {
                    huella |= 1;
                }
            }
        }
        return huella;
    }

    /** Se reduce en pasos de la mitad: de golpe, el bilineal solo miraría unos pocos píxeles del original. */
    private static BufferedImage reducirEnGris(BufferedImage imagen) {
        BufferedImage actual = imagen;
        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        while (ancho / 2 >= ANCHO * 2 && alto / 2 >= ALTO * 2) {
            ancho /= 2;
            alto /= 2;
            actual = escalar(actual, ancho, alto, BufferedImage.TYPE_INT_RGB);
        }
        return escalar(actual, ANCHO, ALTO, BufferedImage.TYPE_BYTE_GRAY);
    }

    private static BufferedImage escalar(BufferedImage origen, int ancho, int alto, int tipo) {
        BufferedImage destino = new BufferedImage(ancho, alto, tipo);
        Graphics2D lienzo = destino.createGraphics();
        try {
            lienzo.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            lienzo.drawImage(origen, 0, 0, ancho, alto, null);
        } finally {
            lienzo.dispose();
        }
        return destino;
    }

    private static int brillo(BufferedImage gris, int x, int y) {
        return gris.getRaster().getSample(x, y, 0);
    }
}
