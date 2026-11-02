package com.benchify.usuario.infrastructure.persistence;

import com.benchify.banco.application.port.out.BancoRepository;
import com.benchify.banco.domain.Banco;
import com.benchify.banco.domain.DatosBanco;
import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.Rol;
import com.benchify.shared.domain.Ubicacion;
import com.benchify.support.AbstractIntegrationTest;
import com.benchify.usuario.application.port.out.ActividadPersonalPort;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class BajaDeCuentaIntegrationTest extends AbstractIntegrationTest {

    private static final Instant AHORA = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private ActividadPersonalPort actividadPersonal;
    @Autowired
    private BancoRepository bancos;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void anonymizesTheProfileAndDeletesOnlyPrivateActivity() {
        Usuario ana = usuarios.registrarSiNoExiste(Usuario.registrar(
                new IdentidadUsuario("uid-baja", "ana@example.com", "Ana", "https://img/a.png", Rol.USER), AHORA));
        UUID bancoId = bancos.save(Banco.proponer(UUID.randomUUID(),
                new DatosBanco("De Ana", null, new Ubicacion(10, 10), null, null, null), Actor.user("uid-baja"), AHORA)).id();
        jdbc.update("INSERT INTO favoritos (usuario_id, banco_id) VALUES ('uid-baja', ?)", bancoId);
        jdbc.update("INSERT INTO checkins (id, usuario_id, banco_id, lat, lng, distancia_metros) VALUES (?, 'uid-baja', ?, 10, 10, 3)",
                UUID.randomUUID(), bancoId);

        usuarios.anonimizar(ana.eliminar(AHORA));
        actividadPersonal.borrar("uid-baja");

        Usuario guardado = usuarios.findById("uid-baja").orElseThrow();
        assertThat(guardado.estaEliminado()).isTrue();
        assertThat(guardado.nombre()).isEqualTo(Usuario.NOMBRE_ELIMINADO);
        assertThat(guardado.email()).isNull();
        assertThat(guardado.avatarUrl()).isNull();
        assertThat(contar("favoritos")).isZero();
        assertThat(contar("checkins")).isZero();
        // Lo publicado sigue ahí, ahora de «Usuario eliminado»
        assertThat(bancos.findById(bancoId)).isPresent();
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE usuario_id = 'uid-baja'", Integer.class);
    }
}
