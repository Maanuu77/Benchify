package com.benchify.shared.domain;


/** Punto WGS84 (EPSG:4326). */
public record Ubicacion(double lat, double lng) {

    private static final double RADIO_TIERRA_METROS = 6_371_008.8;

    public Ubicacion {
        if (Double.isNaN(lat) || lat < -90 || lat > 90) {
            throw new BusinessRuleException("UBICACION_INVALIDA", "La latitud debe estar entre -90 y 90");
        }
        if (Double.isNaN(lng) || lng < -180 || lng > 180) {
            throw new BusinessRuleException("UBICACION_INVALIDA", "La longitud debe estar entre -180 y 180");
        }
    }

    /** Distancia por la fórmula del haversine; suficiente para distancias de metros a kilómetros. */
    public double distanciaMetrosA(Ubicacion otra) {
        double dLat = Math.toRadians(otra.lat - lat);
        double dLng = Math.toRadians(otra.lng - lng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(otra.lat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RADIO_TIERRA_METROS * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
