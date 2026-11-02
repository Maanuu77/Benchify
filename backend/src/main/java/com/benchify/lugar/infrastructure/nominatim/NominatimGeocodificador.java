package com.benchify.lugar.infrastructure.nominatim;

import com.benchify.lugar.application.port.out.Geocodificador;
import com.benchify.lugar.domain.Busqueda;
import com.benchify.lugar.domain.Encuadre;
import com.benchify.lugar.domain.Lugar;
import com.benchify.shared.domain.ServiceUnavailableException;
import com.benchify.shared.domain.Ubicacion;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Geocodificación con Nominatim (OpenStreetMap). Todas las búsquedas comparten un mismo turno de
 * una petición por {@code intervalo}; las repetidas se sirven de la caché sin llamar al servicio.
 */
class NominatimGeocodificador implements Geocodificador {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocodificador.class);
    static final String CODE = "GEOCODIFICACION_NO_DISPONIBLE";
    /** Margen alrededor del centro del mapa en el que se priorizan resultados (unos 30 km). */
    private static final double MARGEN_GRADOS = 0.3;
    /** Resultados de más que se piden para seguir teniendo {@code maximo} tras quitar los repetidos. */
    private static final int EXTRA_POR_REPETIDOS = 3;

    private final RestClient http;
    private final GeocodificacionProperties properties;
    private final Bucket turno;
    private final Cache<String, List<Lugar>> resultados;

    NominatimGeocodificador(RestClient.Builder builder, GeocodificacionProperties properties) {
        this.http = builder.baseUrl(properties.url())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .build();
        this.properties = properties;
        this.turno = Bucket.builder()
                .addLimit(limit -> limit.capacity(1).refillIntervally(1, properties.intervalo()))
                .build();
        this.resultados = Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterWrite(properties.cache())
                .build();
    }

    @Override
    public List<Lugar> buscar(Busqueda busqueda, int maximo) {
        String clave = busqueda.clave() + "|" + maximo;
        List<Lugar> guardados = resultados.getIfPresent(clave);
        if (guardados != null) {
            return guardados;
        }
        esperarTurno();
        List<Lugar> lugares = consultar(busqueda, maximo);
        resultados.put(clave, lugares);
        return lugares;
    }

    private void esperarTurno() {
        try {
            if (!turno.asBlocking().tryConsume(1, properties.esperaMaxima())) {
                throw new ServiceUnavailableException(CODE, "Hay muchas búsquedas ahora mismo; prueba en unos segundos");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException(CODE, "La búsqueda se ha interrumpido");
        }
    }

    private List<Lugar> consultar(Busqueda busqueda, int maximo) {
        try {
            Resultado[] respuesta = http.get()
                    .uri(uri -> {
                        uri.path("/search")
                                .queryParam("q", busqueda.texto())
                                .queryParam("format", "jsonv2")
                                .queryParam("limit", maximo + EXTRA_POR_REPETIDOS)
                                .queryParam("accept-language", busqueda.idioma());
                        Ubicacion cerca = busqueda.cerca();
                        if (cerca != null) {
                            // viewbox: oeste, norte, este, sur; sin bounded solo prioriza, no excluye
                            uri.queryParam("viewbox", String.join(",",
                                    grados(cerca.lng() - MARGEN_GRADOS), grados(cerca.lat() + MARGEN_GRADOS),
                                    grados(cerca.lng() + MARGEN_GRADOS), grados(cerca.lat() - MARGEN_GRADOS)));
                        }
                        return uri.build();
                    })
                    .retrieve()
                    .body(Resultado[].class);
            if (respuesta == null) {
                return List.of();
            }
            // Una plaza puede llegar varias veces (como lugar, como calle, como área peatonal...) con el
            // mismo texto: para quien busca son el mismo resultado
            Map<String, Lugar> distintos = new LinkedHashMap<>();
            Arrays.stream(respuesta).map(Resultado::aLugar).filter(Objects::nonNull)
                    .forEach(lugar -> distintos.putIfAbsent(lugar.nombre() + "|" + lugar.direccion(), lugar));
            return distintos.values().stream().limit(maximo).toList();
        } catch (RestClientException e) {
            log.warn("Nominatim no ha respondido a una búsqueda: {}", e.getMessage());
            throw new ServiceUnavailableException(CODE, "El buscador de direcciones no responde; prueba más tarde");
        }
    }

    /** Seis decimales (unos 10 cm) bastan y evitan arrastrar errores de coma flotante a la URL. */
    private static String grados(double valor) {
        return Double.toString(Math.round(valor * 1e6) / 1e6);
    }

    /** Lo que usamos de cada resultado de {@code /search?format=jsonv2}. */
    record Resultado(String lat, String lon, String name, @JsonProperty("display_name") String displayName,
            List<String> boundingbox) {

        /** null si llega algo que no se puede interpretar: ese resultado se descarta. */
        Lugar aLugar() {
            try {
                String direccion = displayName == null ? "" : displayName;
                String nombre = name != null && !name.isBlank() ? name : direccion.split(",")[0].strip();
                return new Lugar(nombre, direccion, new Ubicacion(Double.parseDouble(lat), Double.parseDouble(lon)),
                        encuadre());
            } catch (RuntimeException e) {
                return null;
            }
        }

        /** Nominatim da el encuadre como [sur, norte, oeste, este]. */
        private Encuadre encuadre() {
            if (boundingbox == null || boundingbox.size() != 4) {
                return null;
            }
            return new Encuadre(Double.parseDouble(boundingbox.get(0)), Double.parseDouble(boundingbox.get(2)),
                    Double.parseDouble(boundingbox.get(1)), Double.parseDouble(boundingbox.get(3)));
        }
    }
}
