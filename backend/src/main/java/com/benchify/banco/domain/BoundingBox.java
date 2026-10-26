package com.benchify.banco.domain;

import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.Ubicacion;

/** Rectángulo visible del mapa. No cruza el antimeridiano. */
public record BoundingBox(double minLat, double minLng, double maxLat, double maxLng) {

    public BoundingBox {
        new Ubicacion(minLat, minLng);
        new Ubicacion(maxLat, maxLng);
        if (minLat >= maxLat || minLng >= maxLng) {
            throw new BusinessRuleException("BOUNDING_BOX_INVALIDA",
                    "El mínimo de latitud y longitud debe ser menor que el máximo");
        }
    }
}
