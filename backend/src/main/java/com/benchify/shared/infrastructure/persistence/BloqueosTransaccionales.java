package com.benchify.shared.infrastructure.persistence;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;

/**
 * Bloqueos consultivos de PostgreSQL por clave ({@code pg_advisory_xact_lock}). Serializan las
 * operaciones de tipo "comprobar y después insertar" que ninguna restricción de la base de datos
 * puede expresar (p. ej. un intervalo mínimo entre dos filas). Se liberan solos al terminar la
 * transacción.
 */
public final class BloqueosTransaccionales {

    private BloqueosTransaccionales() {
    }

    /**
     * Espera hasta obtener el bloqueo de {@code clave}. Debe ejecutarse en una sentencia anterior a
     * las lecturas que protege: en READ COMMITTED cada sentencia ve lo confirmado cuando empieza, así
     * que la siguiente ya ve lo que escribió quien tenía el bloqueo.
     *
     * @throws IllegalStateException si no hay transacción: el bloqueo se soltaría al momento
     */
    public static void bloquear(NamedParameterJdbcTemplate jdbc, String clave) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Un bloqueo transaccional necesita una transacción activa");
        }
        jdbc.execute("SELECT pg_advisory_xact_lock(hashtextextended(:clave, 0))",
                new MapSqlParameterSource("clave", clave), PreparedStatement::execute);
    }
}
