package com.benchify.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Inserta filas de apoyo directamente por SQL para no acoplar los tests de una feature a otra. */
@Component
public class TestData {

    private final JdbcTemplate jdbc;

    public TestData(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void usuario(String id) {
        usuario(id, "Usuario " + id);
    }

    public void usuario(String id, String nombre) {
        jdbc.update("""
                INSERT INTO usuarios (id, email, nombre, rol) VALUES (?, ?, ?, 'USER')
                ON CONFLICT (id) DO NOTHING
                """, id, id + "@benchify.test", nombre);
    }

    public long etiquetaId(String nombre) {
        Long id = jdbc.queryForObject("SELECT id FROM etiquetas WHERE nombre = ?", Long.class, nombre);
        if (id == null) {
            throw new IllegalArgumentException("Unknown tag " + nombre);
        }
        return id;
    }
}
