package com.benchify.usuario.infrastructure.persistence;

import com.benchify.usuario.application.port.out.EstadisticasUsuarioPort;
import com.benchify.usuario.domain.EstadisticasUsuario;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class EstadisticasUsuarioJdbcAdapter implements EstadisticasUsuarioPort {

    private final NamedParameterJdbcTemplate jdbc;

    EstadisticasUsuarioJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public EstadisticasUsuario contar(String usuarioId) {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT count(*) FROM bancos WHERE creado_por = :id AND estado = 'PUBLICADO') AS bancos,
                    (SELECT count(*) FROM resenas WHERE usuario_id = :id) AS resenas,
                    (SELECT count(*) FROM favoritos WHERE usuario_id = :id) AS favoritos,
                    (SELECT count(*) FROM checkins WHERE usuario_id = :id) AS checkins
                """, new MapSqlParameterSource("id", usuarioId),
                (rs, i) -> new EstadisticasUsuario(rs.getLong("bancos"), rs.getLong("resenas"),
                        rs.getLong("favoritos"), rs.getLong("checkins")));
    }
}
