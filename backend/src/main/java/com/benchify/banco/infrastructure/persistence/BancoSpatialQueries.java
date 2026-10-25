package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.domain.BoundingBox;
import com.benchify.banco.domain.FiltroBancos;
import com.benchify.banco.domain.Ubicacion;
import com.benchify.shared.domain.PageQuery;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Consultas PostGIS. Devuelven solo identificadores (y distancia); el adaptador carga después
 * las entidades con JPA. Los filtros se añaden como fragmentos fijos con parámetros con nombre,
 * nunca concatenando valores.
 */
@Component
class BancoSpatialQueries {

    private static final String PUNTO = "ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography";

    private final NamedParameterJdbcTemplate jdbc;

    BancoSpatialQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<UUID> idsEnArea(BoundingBox area, FiltroBancos filtro, int limite) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("minLat", area.minLat()).addValue("minLng", area.minLng())
                .addValue("maxLat", area.maxLat()).addValue("maxLng", area.maxLng())
                .addValue("limite", limite);
        String sql = """
                SELECT b.id FROM bancos b
                WHERE b.estado = 'PUBLICADO'
                  AND b.ubicacion::geometry && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
                """ + filtros(filtro, params) + """
                ORDER BY b.puntuacion_media DESC, b.num_resenas DESC, b.id
                LIMIT :limite
                """;
        return jdbc.queryForList(sql, params, UUID.class);
    }

    List<IdConDistancia> idsCercanos(Ubicacion centro, int radioMetros, FiltroBancos filtro, PageQuery page) {
        MapSqlParameterSource params = paramsCercania(centro, radioMetros)
                .addValue("limite", page.size())
                .addValue("offset", page.offset());
        String sql = """
                SELECT b.id, ST_Distance(b.ubicacion, %s) AS distancia FROM bancos b
                WHERE b.estado = 'PUBLICADO'
                  AND ST_DWithin(b.ubicacion, %s, :radio)
                """.formatted(PUNTO, PUNTO) + filtros(filtro, params) + """
                ORDER BY distancia, b.id
                LIMIT :limite OFFSET :offset
                """;
        return jdbc.query(sql, params,
                (rs, i) -> new IdConDistancia(rs.getObject("id", UUID.class), rs.getDouble("distancia")));
    }

    long contarCercanos(Ubicacion centro, int radioMetros, FiltroBancos filtro) {
        MapSqlParameterSource params = paramsCercania(centro, radioMetros);
        String sql = """
                SELECT count(*) FROM bancos b
                WHERE b.estado = 'PUBLICADO'
                  AND ST_DWithin(b.ubicacion, %s, :radio)
                """.formatted(PUNTO) + filtros(filtro, params);
        Long total = jdbc.queryForObject(sql, params, Long.class);
        return total == null ? 0 : total;
    }

    private static MapSqlParameterSource paramsCercania(Ubicacion centro, int radioMetros) {
        return new MapSqlParameterSource()
                .addValue("lat", centro.lat())
                .addValue("lng", centro.lng())
                .addValue("radio", radioMetros);
    }

    private static String filtros(FiltroBancos filtro, MapSqlParameterSource params) {
        StringBuilder sql = new StringBuilder();
        if (filtro.accesible() != null) {
            sql.append("  AND b.accesible = :accesible\n");
            params.addValue("accesible", filtro.accesible());
        }
        if (filtro.conSombra() != null) {
            sql.append("  AND b.con_sombra = :conSombra\n");
            params.addValue("conSombra", filtro.conSombra());
        }
        if (filtro.conRespaldo() != null) {
            sql.append("  AND b.con_respaldo = :conRespaldo\n");
            params.addValue("conRespaldo", filtro.conRespaldo());
        }
        if (filtro.puntuacionMinima() != null) {
            sql.append("  AND b.puntuacion_media >= :puntuacionMinima\n");
            params.addValue("puntuacionMinima", filtro.puntuacionMinima());
        }
        if (filtro.etiquetaId() != null) {
            sql.append("  AND EXISTS (SELECT 1 FROM banco_etiquetas be"
                    + " WHERE be.banco_id = b.id AND be.etiqueta_id = :etiquetaId)\n");
            params.addValue("etiquetaId", filtro.etiquetaId());
        }
        return sql.toString();
    }

    record IdConDistancia(UUID id, double distancia) {
    }
}
