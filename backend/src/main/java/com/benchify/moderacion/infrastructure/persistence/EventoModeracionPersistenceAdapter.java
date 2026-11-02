package com.benchify.moderacion.infrastructure.persistence;

import com.benchify.moderacion.application.port.out.EventoModeracionRepository;
import com.benchify.moderacion.domain.AccionModeracion;
import com.benchify.moderacion.domain.EventoModeracion;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/** Registro de solo escritura y lectura paginada: SQL directo, sin entidad JPA. */
@Component
class EventoModeracionPersistenceAdapter implements EventoModeracionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    EventoModeracionPersistenceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(EventoModeracion evento) {
        jdbc.update("""
                INSERT INTO eventos_moderacion (id, moderador_id, accion, recurso_tipo, recurso_id, motivo, fecha)
                VALUES (:id, :moderador, :accion, :tipo, :recurso, :motivo, :fecha)
                """, new MapSqlParameterSource("id", evento.id())
                .addValue("moderador", evento.moderadorId())
                .addValue("accion", evento.accion().name())
                .addValue("tipo", evento.recursoTipo().name())
                .addValue("recurso", evento.recursoId())
                .addValue("motivo", evento.motivo())
                .addValue("fecha", Timestamp.from(evento.fecha())));
    }

    @Override
    public PageResult<EventoModeracion> findRecientes(PageQuery page) {
        List<EventoModeracion> contenido = jdbc.query("""
                SELECT id, moderador_id, accion, recurso_id, motivo, fecha FROM eventos_moderacion
                ORDER BY fecha DESC, id LIMIT :limite OFFSET :offset
                """, new MapSqlParameterSource("limite", page.size()).addValue("offset", page.offset()),
                (rs, i) -> new EventoModeracion(rs.getObject("id", UUID.class), rs.getString("moderador_id"),
                        AccionModeracion.valueOf(rs.getString("accion")), rs.getObject("recurso_id", UUID.class),
                        rs.getString("motivo"), rs.getTimestamp("fecha").toInstant()));
        Long total = jdbc.queryForObject("SELECT count(*) FROM eventos_moderacion", new MapSqlParameterSource(), Long.class);
        return new PageResult<>(contenido, page.page(), page.size(), total == null ? 0 : total);
    }
}
