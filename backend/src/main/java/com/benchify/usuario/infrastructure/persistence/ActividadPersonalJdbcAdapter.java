package com.benchify.usuario.infrastructure.persistence;

import com.benchify.usuario.application.port.out.ActividadPersonalPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Borra en SQL la actividad privada de un usuario dado de baja. Favoritos y check-ins son de otras
 * features, pero al usuario solo le importa que desaparezcan: no hace falta pasar por su dominio.
 */
@Component
class ActividadPersonalJdbcAdapter implements ActividadPersonalPort {

    private final NamedParameterJdbcTemplate jdbc;

    ActividadPersonalJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void borrar(String usuarioId) {
        MapSqlParameterSource id = new MapSqlParameterSource("id", usuarioId);
        jdbc.update("DELETE FROM favoritos WHERE usuario_id = :id", id);
        jdbc.update("DELETE FROM checkins WHERE usuario_id = :id", id);
    }
}
