package com.benchify.banco.infrastructure.persistence;

import com.benchify.banco.application.port.out.RevisionBancoRepository;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.banco.domain.EstadoRevision;
import com.benchify.banco.domain.RevisionBanco;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Los datos propuestos se guardan tal cual en JSONB: solo se leen enteros para enseñarlos al
 * moderador y, si se aprueban, copiarlos al banco.
 */
@Component
class RevisionBancoPersistenceAdapter implements RevisionBancoRepository {

    private static final String COLUMNAS =
            "r.id, r.banco_id, r.autor_id, r.datos, r.estado, r.fecha, r.revisada_por, r.fecha_revision";
    /** Las de bancos eliminados no se pueden aplicar: no salen en la cola. */
    private static final String PENDIENTES = """
            FROM revisiones_banco r JOIN bancos b ON b.id = r.banco_id
            WHERE r.estado = 'PENDIENTE' AND b.estado <> 'ELIMINADO'
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;
    private final RowMapper<RevisionBanco> fila = this::leer;

    RevisionBancoPersistenceAdapter(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void guardar(RevisionBanco r) {
        jdbc.update("""
                INSERT INTO revisiones_banco (id, banco_id, autor_id, datos, estado, fecha, revisada_por, fecha_revision)
                VALUES (:id, :banco, :autor, CAST(:datos AS jsonb), :estado, :fecha, :revisadaPor, :fechaRevision)
                ON CONFLICT (id) DO UPDATE SET datos = EXCLUDED.datos, estado = EXCLUDED.estado, fecha = EXCLUDED.fecha,
                    revisada_por = EXCLUDED.revisada_por, fecha_revision = EXCLUDED.fecha_revision
                """, new MapSqlParameterSource("id", r.id())
                .addValue("banco", r.bancoId())
                .addValue("autor", r.autorId())
                .addValue("datos", json.writeValueAsString(r.datos()))
                .addValue("estado", r.estado().name())
                .addValue("fecha", Timestamp.from(r.fecha()))
                .addValue("revisadaPor", r.revisadaPor())
                .addValue("fechaRevision", r.fechaRevision() == null ? null : Timestamp.from(r.fechaRevision())));
    }

    @Override
    public Optional<RevisionBanco> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM revisiones_banco r WHERE r.id = :id",
                new MapSqlParameterSource("id", id), fila).stream().findFirst();
    }

    @Override
    public Optional<RevisionBanco> findPendienteByBanco(UUID bancoId) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM revisiones_banco r WHERE r.banco_id = :banco AND r.estado = 'PENDIENTE'",
                new MapSqlParameterSource("banco", bancoId), fila).stream().findFirst();
    }

    @Override
    public PageResult<RevisionBanco> findPendientes(PageQuery page) {
        List<RevisionBanco> contenido = jdbc.query("SELECT " + COLUMNAS + " " + PENDIENTES
                        + "ORDER BY r.fecha, r.id LIMIT :limite OFFSET :offset",
                new MapSqlParameterSource("limite", page.size()).addValue("offset", page.offset()), fila);
        return new PageResult<>(contenido, page.page(), page.size(), countPendientes());
    }

    @Override
    public long countPendientes() {
        Long total = jdbc.queryForObject("SELECT count(*) " + PENDIENTES, new MapSqlParameterSource(), Long.class);
        return total == null ? 0 : total;
    }

    private RevisionBanco leer(ResultSet rs, int fila) throws SQLException {
        return new RevisionBanco(
                rs.getObject("id", UUID.class),
                rs.getObject("banco_id", UUID.class),
                rs.getString("autor_id"),
                json.readValue(rs.getString("datos"), DatosBanco.class),
                EstadoRevision.valueOf(rs.getString("estado")),
                rs.getTimestamp("fecha").toInstant(),
                rs.getString("revisada_por"),
                instante(rs.getTimestamp("fecha_revision")));
    }

    private static Instant instante(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
