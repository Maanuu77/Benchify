package com.benchify.checkin.infrastructure.persistence;

import com.benchify.checkin.application.port.out.CheckInRepository;
import com.benchify.checkin.domain.CheckIn;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.shared.infrastructure.persistence.BloqueosTransaccionales;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Los check-ins solo se insertan y se leen: SQL directo, sin entidad JPA. */
@Component
class CheckInPersistenceAdapter implements CheckInRepository {

    private static final RowMapper<CheckIn> ROW_MAPPER = (rs, i) -> new CheckIn(
            rs.getObject("id", UUID.class),
            rs.getString("usuario_id"),
            rs.getObject("banco_id", UUID.class),
            new Ubicacion(rs.getDouble("lat"), rs.getDouble("lng")),
            rs.getDouble("distancia_metros"),
            rs.getTimestamp("fecha").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    CheckInPersistenceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CheckIn save(CheckIn checkIn) {
        jdbc.update("""
                INSERT INTO checkins (id, usuario_id, banco_id, lat, lng, distancia_metros, fecha)
                VALUES (:id, :usuario, :banco, :lat, :lng, :distancia, :fecha)
                """, new MapSqlParameterSource()
                .addValue("id", checkIn.id())
                .addValue("usuario", checkIn.usuarioId())
                .addValue("banco", checkIn.bancoId())
                .addValue("lat", checkIn.posicion().lat())
                .addValue("lng", checkIn.posicion().lng())
                .addValue("distancia", checkIn.distanciaMetros())
                .addValue("fecha", Timestamp.from(checkIn.fecha())));
        return checkIn;
    }

    @Override
    public Optional<Instant> findUltimaFechaForUpdate(String usuarioId, UUID bancoId) {
        BloqueosTransaccionales.bloquear(jdbc, "checkin:" + usuarioId + ":" + bancoId);
        Timestamp ultima = jdbc.queryForObject(
                "SELECT max(fecha) FROM checkins WHERE usuario_id = :usuario AND banco_id = :banco",
                new MapSqlParameterSource("usuario", usuarioId).addValue("banco", bancoId), Timestamp.class);
        return Optional.ofNullable(ultima).map(Timestamp::toInstant);
    }

    @Override
    public PageResult<CheckIn> findByUsuario(String usuarioId, PageQuery page) {
        MapSqlParameterSource params = new MapSqlParameterSource("usuario", usuarioId)
                .addValue("limite", page.size())
                .addValue("offset", page.offset());
        List<CheckIn> contenido = jdbc.query("""
                SELECT * FROM checkins WHERE usuario_id = :usuario
                ORDER BY fecha DESC, id LIMIT :limite OFFSET :offset
                """, params, ROW_MAPPER);
        Long total = jdbc.queryForObject("SELECT count(*) FROM checkins WHERE usuario_id = :usuario", params, Long.class);
        return new PageResult<>(contenido, page.page(), page.size(), total == null ? 0 : total);
    }

    @Override
    public long countByBanco(UUID bancoId) {
        Long total = jdbc.queryForObject("SELECT count(*) FROM checkins WHERE banco_id = :banco",
                new MapSqlParameterSource("banco", bancoId), Long.class);
        return total == null ? 0 : total;
    }
}
