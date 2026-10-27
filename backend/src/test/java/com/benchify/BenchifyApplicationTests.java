package com.benchify;

import com.benchify.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class BenchifyApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndMigrationsEnablePostgis() {
        String version = jdbcTemplate.queryForObject("SELECT postgis_lib_version()", String.class);

        assertThat(version).startsWith("3.");
    }
}
