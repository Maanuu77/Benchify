package com.benchify.usuario.infrastructure.persistence;

import com.benchify.support.AbstractIntegrationTest;
import com.benchify.support.TestData;
import com.benchify.usuario.application.port.out.EstadisticasUsuarioPort;
import com.benchify.usuario.domain.EstadisticasUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EstadisticasUsuarioJdbcAdapterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EstadisticasUsuarioPort estadisticas;
    @Autowired
    private TestData testData;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void countsActivityAcrossFeatures() {
        testData.usuario("est-ana");
        testData.usuario("est-luis");
        UUID publicado = banco("est-ana", "PUBLICADO");
        banco("est-ana", "PENDIENTE");
        UUID deLuis = banco("est-luis", "PUBLICADO");
        jdbc.update("INSERT INTO resenas (id, banco_id, usuario_id, puntuacion) VALUES (?, ?, 'est-ana', 4)",
                UUID.randomUUID(), deLuis);
        jdbc.update("INSERT INTO favoritos (usuario_id, banco_id) VALUES ('est-ana', ?), ('est-ana', ?)",
                publicado, deLuis);
        jdbc.update("INSERT INTO checkins (id, usuario_id, banco_id, lat, lng, distancia_metros) "
                + "VALUES (?, 'est-ana', ?, 0, 0, 1)", UUID.randomUUID(), deLuis);

        assertThat(estadisticas.contar("est-ana")).isEqualTo(new EstadisticasUsuario(1, 1, 2, 1));
        assertThat(estadisticas.contar("est-luis")).isEqualTo(new EstadisticasUsuario(1, 0, 0, 0));
    }

    private UUID banco(String autor, String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO bancos (id, titulo, lat, lng, estado, creado_por) VALUES (?, 'B', 0, 0, ?, ?)",
                id, estado, autor);
        return id;
    }
}
