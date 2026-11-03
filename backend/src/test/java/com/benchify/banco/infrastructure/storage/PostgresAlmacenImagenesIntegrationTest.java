package com.benchify.banco.infrastructure.storage;

import com.benchify.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PostgresAlmacenImagenesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private PostgresAlmacenImagenes almacen;

    @BeforeEach
    void almacen() {
        almacen = new PostgresAlmacenImagenes(jdbc, "https://api.benchify.test/");
    }

    @Test
    void storesReadsReplacesAndDeletesFiles() {
        String ruta = "bancos/" + UUID.randomUUID() + "/foto.jpg";

        String url = almacen.guardar(ruta, new byte[]{1, 2, 3}, "image/jpeg");

        assertThat(url).isEqualTo("https://api.benchify.test/api/v1/ficheros/" + ruta);
        assertThat(almacen.leer(ruta)).hasValueSatisfying(b -> assertThat(b).containsExactly(1, 2, 3));

        almacen.guardar(ruta, new byte[]{9}, "image/jpeg");
        assertThat(almacen.leer(ruta)).hasValueSatisfying(b -> assertThat(b).containsExactly(9));

        almacen.borrar(ruta);
        almacen.borrar(ruta);
        assertThat(almacen.leer(ruta)).isEmpty();
    }
}
