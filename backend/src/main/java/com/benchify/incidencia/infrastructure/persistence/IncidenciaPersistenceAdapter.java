package com.benchify.incidencia.infrastructure.persistence;

import com.benchify.incidencia.application.port.out.IncidenciaRepository;
import com.benchify.incidencia.domain.EstadoIncidencia;
import com.benchify.incidencia.domain.Incidencia;
import com.benchify.incidencia.domain.TipoIncidencia;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Tabla sencilla de avisos: SQL directo, sin entidad JPA. */
@Component
class IncidenciaPersistenceAdapter implements IncidenciaRepository {

    private static final String COLUMNAS =
            "id, banco_id, usuario_id, tipo, comentario, estado, fecha, resuelta_por, fecha_resolucion";
    private static final RowMapper<Incidencia> FILA = (rs, i) -> new Incidencia(
            rs.getObject("id", UUID.class), rs.getObject("banco_id", UUID.class), rs.getString("usuario_id"),
            TipoIncidencia.valueOf(rs.getString("tipo")), rs.getString("comentario"),
            EstadoIncidencia.valueOf(rs.getString("estado")), rs.getTimestamp("fecha").toInstant(),
            rs.getString("resuelta_por"),
            rs.getTimestamp("fecha_resolucion") == null ? null : rs.getTimestamp("fecha_resolucion").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    IncidenciaPersistenceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Incidencia i) {
        jdbc.update("INSERT INTO incidencias (" + COLUMNAS + ") VALUES "
                + "(:id, :banco, :usuario, :tipo, :comentario, :estado, :fecha, :resueltaPor, :fechaResolucion)", params(i));
    }

    @Override
    public void actualizar(Incidencia i) {
        jdbc.update("""
                UPDATE incidencias SET estado = :estado, resuelta_por = :resueltaPor, fecha_resolucion = :fechaResolucion
                WHERE id = :id
                """, params(i));
    }

    @Override
    public Optional<Incidencia> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM incidencias WHERE id = :id",
                new MapSqlParameterSource("id", id), FILA).stream().findFirst();
    }

    @Override
    public boolean existeAbierta(UUID bancoId, String usuarioId) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM incidencias
                               WHERE banco_id = :banco AND usuario_id = :usuario AND estado = 'ABIERTA')
                """, new MapSqlParameterSource("banco", bancoId).addValue("usuario", usuarioId), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    @Override
    public Map<TipoIncidencia, Long> contarAbiertasPorTipo(UUID bancoId) {
        Map<TipoIncidencia, Long> totales = new EnumMap<>(TipoIncidencia.class);
        jdbc.query("""
                SELECT tipo, count(*) AS total FROM incidencias
                WHERE banco_id = :banco AND estado = 'ABIERTA' GROUP BY tipo
                """, new MapSqlParameterSource("banco", bancoId),
                rs -> {
                    totales.put(TipoIncidencia.valueOf(rs.getString("tipo")), rs.getLong("total"));
                });
        return totales;
    }

    @Override
    public PageResult<Incidencia> findAbiertas(PageQuery page) {
        List<Incidencia> contenido = jdbc.query("SELECT " + COLUMNAS + " FROM incidencias WHERE estado = 'ABIERTA' "
                        + "ORDER BY fecha, id LIMIT :limite OFFSET :offset",
                new MapSqlParameterSource("limite", page.size()).addValue("offset", page.offset()), FILA);
        return new PageResult<>(contenido, page.page(), page.size(), contarAbiertas());
    }

    @Override
    public long contarAbiertas() {
        Long total = jdbc.queryForObject("SELECT count(*) FROM incidencias WHERE estado = 'ABIERTA'",
                new MapSqlParameterSource(), Long.class);
        return total == null ? 0 : total;
    }

    private static MapSqlParameterSource params(Incidencia i) {
        return new MapSqlParameterSource("id", i.id())
                .addValue("banco", i.bancoId())
                .addValue("usuario", i.usuarioId())
                .addValue("tipo", i.tipo().name())
                .addValue("comentario", i.comentario())
                .addValue("estado", i.estado().name())
                .addValue("fecha", Timestamp.from(i.fecha()))
                .addValue("resueltaPor", i.resueltaPor())
                .addValue("fechaResolucion", timestamp(i.fechaResolucion()));
    }

    private static Timestamp timestamp(Instant instante) {
        return instante == null ? null : Timestamp.from(instante);
    }
}
