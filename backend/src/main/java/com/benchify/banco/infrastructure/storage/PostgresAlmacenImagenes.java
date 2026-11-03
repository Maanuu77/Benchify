package com.benchify.banco.infrastructure.storage;

import com.benchify.banco.application.port.out.AlmacenImagenesPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Fotos guardadas en la propia base de datos, para el despliegue gratuito (sin Cloud Storage). Las
 * de la carpeta pública se descargan sin autenticación desde {@link FicherosPublicosRouter}; las
 * pendientes, como siempre, a través de la API con el token de quien puede verlas.
 */
class PostgresAlmacenImagenes implements AlmacenImagenesPort {

    static final String RUTA_PUBLICA = "/api/v1/ficheros/";

    private final NamedParameterJdbcTemplate jdbc;
    private final String urlBase;

    PostgresAlmacenImagenes(NamedParameterJdbcTemplate jdbc, String urlPublicaApi) {
        this.jdbc = jdbc;
        this.urlBase = (urlPublicaApi.endsWith("/") ? urlPublicaApi.substring(0, urlPublicaApi.length() - 1) : urlPublicaApi)
                + RUTA_PUBLICA;
    }

    @Override
    public String guardar(String ruta, byte[] contenido, String contentType) {
        jdbc.update("""
                INSERT INTO ficheros_imagen (ruta, contenido, content_type) VALUES (:ruta, :contenido, :tipo)
                ON CONFLICT (ruta) DO UPDATE SET contenido = EXCLUDED.contenido, content_type = EXCLUDED.content_type,
                    fecha = now()
                """, new MapSqlParameterSource("ruta", ruta).addValue("contenido", contenido).addValue("tipo", contentType));
        return urlBase + UriUtils.encodePath(ruta, StandardCharsets.UTF_8);
    }

    @Override
    public Optional<byte[]> leer(String ruta) {
        return jdbc.query("SELECT contenido FROM ficheros_imagen WHERE ruta = :ruta",
                new MapSqlParameterSource("ruta", ruta), (rs, i) -> rs.getBytes("contenido")).stream().findFirst();
    }

    @Override
    public void borrar(String ruta) {
        jdbc.update("DELETE FROM ficheros_imagen WHERE ruta = :ruta", new MapSqlParameterSource("ruta", ruta));
    }
}
