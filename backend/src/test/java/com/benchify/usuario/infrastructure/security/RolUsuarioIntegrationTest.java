package com.benchify.usuario.infrastructure.security;

import com.benchify.support.AbstractIntegrationTest;
import com.benchify.support.RecordingIdentityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static com.benchify.support.StubIdTokenVerifier.bearerAdmin;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Autorización con el rol de la base de datos (#15), petición a petición y en la misma instancia.
 * Sin {@code @Transactional}: los cambios de rol se confirman y la caché se invalida como en
 * producción. Los UID son únicos por test.
 */
@AutoConfigureMockMvc
class RolUsuarioIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_PANEL = "/api/v1/admin/usuarios";

    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private RecordingIdentityProvider proveedorIdentidad;
    @Autowired
    private JdbcTemplate jdbc;

    private String jefa;
    private String ana;

    @BeforeEach
    void usuarios() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        jefa = "jefa-" + sufijo;
        ana = "ana-" + sufijo;
    }

    @Test
    void roleChangesApplyOnTheNextRequestWhateverTheTokenSays() {
        // Alta con el claim ADMIN: así entra el primer administrador
        assertThat(mvc.get().uri(ADMIN_PANEL).header("Authorization", bearerAdmin(jefa))).hasStatusOk();
        assertThat(mvc.get().uri(ADMIN_PANEL).header("Authorization", bearer(ana))).hasStatus(403);

        cambiarRol(ana, "ADMIN");
        // Su token aún dice USER, pero manda la base de datos
        assertThat(mvc.get().uri(ADMIN_PANEL).header("Authorization", bearer(ana))).hasStatusOk();

        cambiarRol(ana, "USER");
        // Con un token emitido mientras era ADMIN pierde el acceso en la siguiente petición
        assertThat(mvc.get().uri(ADMIN_PANEL).header("Authorization", bearerAdmin(ana))).hasStatus(403);
        assertThat(mvc.put().uri(ADMIN_PANEL + "/{id}/rol", jefa).header("Authorization", bearerAdmin(ana))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"USER\"}")).hasStatus(403);

        // Ese token antiguo no ha vuelto a escribir ADMIN en la base de datos. Firebase se actualiza tras
        // cada cambio y, cuando llega un token con otro rol, se le pide que corrija el claim
        assertThat(rolGuardado(ana)).isEqualTo("USER");
        assertThat(proveedorIdentidad.llamadas(ana)).containsExactly(
                "asignarRol:ADMIN", "revocarSesiones", "corregirRol:ADMIN",
                "asignarRol:USER", "revocarSesiones", "corregirRol:USER");
    }

    @Test
    void anAdministratorCannotDemoteThemselves() {
        assertThat(mvc.put().uri(ADMIN_PANEL + "/{id}/rol", jefa).header("Authorization", bearerAdmin(jefa))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"USER\"}"))
                .hasStatus(422)
                .bodyJson().extractingPath("$.code").isEqualTo("USUARIO_AUTODEGRADACION");
        assertThat(rolGuardado(jefa)).isEqualTo("ADMIN");
    }

    @Test
    void anonymousRequestsKeepWorking() {
        assertThat(mvc.get().uri("/api/v1/etiquetas")).hasStatusOk();
        assertThat(mvc.get().uri(ADMIN_PANEL)).hasStatus(401);
    }

    private void cambiarRol(String usuarioId, String rol) {
        assertThat(mvc.put().uri(ADMIN_PANEL + "/{id}/rol", usuarioId).header("Authorization", bearerAdmin(jefa))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"" + rol + "\"}"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.rol").isEqualTo(rol);
    }

    private String rolGuardado(String usuarioId) {
        return jdbc.queryForObject("SELECT rol FROM usuarios WHERE id = ?", String.class, usuarioId);
    }
}
