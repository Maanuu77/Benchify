package com.benchify.notificacion.infrastructure.persistence;

import com.benchify.notificacion.application.port.out.NotificacionRepository;
import com.benchify.notificacion.domain.Notificacion;
import com.benchify.notificacion.domain.TipoNotificacion;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Tabla sencilla de avisos: SQL directo, sin entidad JPA. */
@Component
class NotificacionPersistenceAdapter implements NotificacionRepository {

    private static final String COLUMNAS = "id, usuario_id, tipo, titulo, mensaje, enlace, leida, fecha";
    private static final RowMapper<Notificacion> FILA = (rs, i) -> new Notificacion(
            rs.getObject("id", UUID.class), rs.getString("usuario_id"), TipoNotificacion.valueOf(rs.getString("tipo")),
            rs.getString("titulo"), rs.getString("mensaje"), rs.getString("enlace"), rs.getBoolean("leida"),
            rs.getTimestamp("fecha").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    NotificacionPersistenceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Notificacion n) {
        jdbc.update("INSERT INTO notificaciones (" + COLUMNAS + ") "
                        + "VALUES (:id, :usuario, :tipo, :titulo, :mensaje, :enlace, :leida, :fecha)",
                new MapSqlParameterSource("id", n.id())
                        .addValue("usuario", n.usuarioId())
                        .addValue("tipo", n.tipo().name())
                        .addValue("titulo", n.titulo())
                        .addValue("mensaje", n.mensaje())
                        .addValue("enlace", n.enlace())
                        .addValue("leida", n.leida())
                        .addValue("fecha", Timestamp.from(n.fecha())));
    }

    @Override
    public Optional<Notificacion> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM notificaciones WHERE id = :id",
                new MapSqlParameterSource("id", id), FILA).stream().findFirst();
    }

    @Override
    public PageResult<Notificacion> findByUsuario(String usuarioId, PageQuery page) {
        MapSqlParameterSource params = new MapSqlParameterSource("usuario", usuarioId)
                .addValue("limite", page.size())
                .addValue("offset", page.offset());
        List<Notificacion> contenido = jdbc.query("SELECT " + COLUMNAS + " FROM notificaciones "
                + "WHERE usuario_id = :usuario ORDER BY fecha DESC, id LIMIT :limite OFFSET :offset", params, FILA);
        Long total = jdbc.queryForObject("SELECT count(*) FROM notificaciones WHERE usuario_id = :usuario",
                params, Long.class);
        return new PageResult<>(contenido, page.page(), page.size(), total == null ? 0 : total);
    }

    @Override
    public long contarNoLeidas(String usuarioId) {
        Long total = jdbc.queryForObject("SELECT count(*) FROM notificaciones WHERE usuario_id = :usuario AND NOT leida",
                new MapSqlParameterSource("usuario", usuarioId), Long.class);
        return total == null ? 0 : total;
    }

    @Override
    public void marcarLeida(UUID id) {
        jdbc.update("UPDATE notificaciones SET leida = TRUE WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    @Override
    public void marcarTodasLeidas(String usuarioId) {
        jdbc.update("UPDATE notificaciones SET leida = TRUE WHERE usuario_id = :usuario AND NOT leida",
                new MapSqlParameterSource("usuario", usuarioId));
    }
}
