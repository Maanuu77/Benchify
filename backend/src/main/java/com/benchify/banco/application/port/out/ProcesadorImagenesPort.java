package com.benchify.banco.application.port.out;

import com.benchify.banco.domain.ArchivoImagen;
import com.benchify.banco.domain.ImagenProcesada;

/** Normaliza las fotos subidas antes de guardarlas. */
public interface ProcesadorImagenesPort {

    /**
     * Recodifica la foto a JPEG sin metadatos, con la orientación EXIF aplicada y el lado mayor limitado
     * a {@link ImagenProcesada#LADO_MAXIMO}, y genera su miniatura. Es trabajo de CPU: no debe hacerse
     * dentro de una transacción.
     *
     * @throws com.benchify.shared.domain.BusinessRuleException si no se puede decodificar
     *         ({@code IMAGEN_CONTENIDO_INVALIDO}) o supera {@link ImagenProcesada#MAX_PIXELES}
     *         ({@code IMAGEN_RESOLUCION_EXCESIVA})
     */
    ImagenProcesada procesar(ArchivoImagen archivo);
}
