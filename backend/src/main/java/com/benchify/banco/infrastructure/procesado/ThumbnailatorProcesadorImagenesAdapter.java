package com.benchify.banco.infrastructure.procesado;

import com.benchify.banco.application.port.out.ProcesadorImagenesPort;
import com.benchify.banco.domain.ArchivoImagen;
import com.benchify.banco.domain.ImagenProcesada;
import com.benchify.shared.domain.BusinessRuleException;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.util.exif.ExifFilterUtils;
import net.coobird.thumbnailator.util.exif.ExifUtils;
import net.coobird.thumbnailator.util.exif.Orientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;

/**
 * Normaliza las fotos con ImageIO (más el lector WebP de TwelveMonkeys) y Thumbnailator: aplica la
 * orientación EXIF, pinta la transparencia sobre blanco, limita el lado mayor y recodifica a JPEG.
 * Al recodificar se pierden todos los metadatos del original (ubicación GPS, modelo del móvil…).
 */
@Component
class ThumbnailatorProcesadorImagenesAdapter implements ProcesadorImagenesPort {

    static final float CALIDAD_JPEG = 0.85f;

    private static final Logger log = LoggerFactory.getLogger(ThumbnailatorProcesadorImagenesAdapter.class);

    ThumbnailatorProcesadorImagenesAdapter() {
        // Registra los lectores añadidos como dependencia (WebP) aunque ImageIO ya se hubiera inicializado
        ImageIO.scanForPlugins();
    }

    @Override
    public ImagenProcesada procesar(ArchivoImagen archivo) {
        BufferedImage foto = reducir(decodificar(archivo.contenido()), ImagenProcesada.LADO_MAXIMO);
        BufferedImage miniatura = reducir(foto, ImagenProcesada.LADO_MAXIMO_MINIATURA);
        return new ImagenProcesada(jpeg(foto), jpeg(miniatura));
    }

    private static BufferedImage decodificar(byte[] contenido) {
        // En memoria: ImageIO usaría por defecto ficheros temporales como caché
        try (ImageInputStream entrada = new MemoryCacheImageInputStream(new ByteArrayInputStream(contenido))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw contenidoInvalido();
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, false, false);
                return leer(lector);
            } finally {
                lector.dispose();
            }
        } catch (BusinessRuleException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // Ficheros truncados o con variantes que ImageIO no soporta (p. ej. JPEG en CMYK)
            log.debug("Could not decode uploaded image", e);
            throw contenidoInvalido();
        }
    }

    private static BufferedImage leer(ImageReader lector) throws IOException {
        // Las dimensiones salen de la cabecera: la comprobación no reserva memoria para los píxeles
        int ancho = lector.getWidth(0);
        int alto = lector.getHeight(0);
        if ((long) ancho * alto > ImagenProcesada.MAX_PIXELES) {
            throw new BusinessRuleException("IMAGEN_RESOLUCION_EXCESIVA",
                    "La imagen no puede superar " + ImagenProcesada.MAX_PIXELES / 1_000_000 + " megapíxeles");
        }
        Orientation orientacion = orientacionExif(lector);
        // Submuestreo al decodificar: nunca se carga mucha más resolución de la que se va a publicar
        int paso = Math.max(1, Math.max(ancho, alto) / ImagenProcesada.LADO_MAXIMO);
        ImageReadParam parametros = lector.getDefaultReadParam();
        parametros.setSourceSubsampling(paso, paso, 0, 0);
        BufferedImage opaca = aplanar(lector.read(0, parametros));
        return orientacion == null ? opaca : ExifFilterUtils.getFilterForOrientation(orientacion).apply(opaca);
    }

    /** Solo los JPEG de cámara y móvil traen la orientación; una cabecera EXIF dañada no invalida la foto. */
    private static Orientation orientacionExif(ImageReader lector) {
        try {
            return "jpeg".equalsIgnoreCase(lector.getFormatName()) ? ExifUtils.getExifOrientation(lector, 0) : null;
        } catch (IOException | RuntimeException e) {
            log.debug("Ignoring unreadable EXIF orientation", e);
            return null;
        }
    }

    /** JPEG no admite transparencia: se pinta sobre blanco, igual que se vería sobre la ficha. */
    private static BufferedImage aplanar(BufferedImage imagen) {
        if (imagen.getType() == BufferedImage.TYPE_INT_RGB) {
            return imagen;
        }
        BufferedImage opaca = new BufferedImage(imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D lienzo = opaca.createGraphics();
        try {
            lienzo.drawImage(imagen, 0, 0, Color.WHITE, null);
        } finally {
            lienzo.dispose();
        }
        return opaca;
    }

    /** Reduce sin deformar hasta que el lado mayor mida {@code ladoMaximo}; nunca amplía. */
    private static BufferedImage reducir(BufferedImage imagen, int ladoMaximo) {
        if (Math.max(imagen.getWidth(), imagen.getHeight()) <= ladoMaximo) {
            return imagen;
        }
        try {
            return Thumbnails.of(imagen)
                    .size(ladoMaximo, ladoMaximo)
                    .imageType(BufferedImage.TYPE_INT_RGB)
                    .asBufferedImage();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not resize image", e);
        }
    }

    /** JPEG sin metadatos: el escritor solo añade la cabecera JFIF mínima. */
    private static byte[] jpeg(BufferedImage imagen) {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ImageOutputStream destino = new MemoryCacheImageOutputStream(salida)) {
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(CALIDAD_JPEG);
            escritor.setOutput(destino);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not encode image as JPEG", e);
        } finally {
            escritor.dispose();
        }
        return salida.toByteArray();
    }

    private static BusinessRuleException contenidoInvalido() {
        return new BusinessRuleException("IMAGEN_CONTENIDO_INVALIDO",
                "No se ha podido leer la imagen; prueba a exportarla de nuevo como JPEG o PNG");
    }
}
