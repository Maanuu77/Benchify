package com.benchify.favorito.infrastructure.persistence;

import com.benchify.favorito.application.port.out.FavoritoRepository;
import com.benchify.favorito.domain.Favorito;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Tabla de unión sin comportamiento propio: se accede con SQL directo en lugar de una entidad JPA.
 * El listado filtra en la propia consulta los bancos que el usuario ya no puede ver, para que el
 * total de la paginación sea exacto.
 */
@Component
class FavoritoPersistenceAdapter implements FavoritoRepository {

    private static final String VISIBLES = """
            FROM favoritos f JOIN bancos b ON b.id = f.banco_id
            WHERE f.usuario_id = :usuario
              AND b.estado <> 'ELIMINADO'
              AND (b.estado = 'PUBLICADO' OR b.creado_por = :usuario)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    FavoritoPersistenceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Favorito favorito) {
        jdbc.update("""
                INSERT INTO favoritos (usuario_id, banco_id, fecha) VALUES (:usuario, :banco, :fecha)
                ON CONFLICT (usuario_id, banco_id) DO NOTHING
                """, params(favorito.usuarioId(), favorito.bancoId())
                .addValue("fecha", Timestamp.from(favorito.fecha())));
    }

    @Override
    public void borrar(String usuarioId, UUID bancoId) {
        jdbc.update("DELETE FROM favoritos WHERE usuario_id = :usuario AND banco_id = :banco",
                params(usuarioId, bancoId));
    }

    @Override
    public boolean existe(String usuarioId, UUID bancoId) {
        Boolean existe = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM favoritos WHERE usuario_id = :usuario AND banco_id = :banco)",
                params(usuarioId, bancoId), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    @Override
    public PageResult<Favorito> findVisiblesByUsuario(String usuarioId, PageQuery page) {
        MapSqlParameterSource params = new MapSqlParameterSource("usuario", usuarioId)
                .addValue("limite", page.size())
                .addValue("offset", page.offset());
        List<Favorito> contenido = jdbc.query(
                "SELECT f.usuario_id, f.banco_id, f.fecha " + VISIBLES
                        + "ORDER BY f.fecha DESC, f.banco_id LIMIT :limite OFFSET :offset",
                params,
                (rs, i) -> new Favorito(rs.getString("usuario_id"), rs.getObject("banco_id", UUID.class),
                        rs.getTimestamp("fecha").toInstant()));
        Long total = jdbc.queryForObject("SELECT count(*) " + VISIBLES, params, Long.class);
        return new PageResult<>(contenido, page.page(), page.size(), total == null ? 0 : total);
    }

    private static MapSqlParameterSource params(String usuarioId, UUID bancoId) {
        return new MapSqlParameterSource("usuario", usuarioId).addValue("banco", bancoId);
    }
}
