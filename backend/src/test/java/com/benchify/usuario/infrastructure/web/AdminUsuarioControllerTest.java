package com.benchify.usuario.infrastructure.web;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Rol;
import com.benchify.support.ApiControllerTest;
import com.benchify.usuario.application.port.in.AdministrarUsuariosUseCase;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static com.benchify.support.StubIdTokenVerifier.bearerAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiControllerTest(AdminUsuarioController.class)
@Import(UsuarioWebMapperImpl.class)
class AdminUsuarioControllerTest {

    private static final Usuario ANA = Usuario.registrar(
            new IdentidadUsuario("ana", "ana@x.com", "Ana", null, Rol.USER), Instant.parse("2026-01-01T00:00:00Z"));

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private AdministrarUsuariosUseCase administrarUsuarios;

    @Test
    void listsUsersWithPagination() {
        when(administrarUsuarios.listar(Actor.admin("root"), new PageQuery(1, 5)))
                .thenReturn(new PageResult<>(List.of(ANA), 1, 5, 6));

        assertThat(mvc.get().uri("/api/v1/admin/usuarios?page=1&size=5").header("Authorization", bearerAdmin("root")))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].id").isEqualTo("ana");
                    assertThat(json).extractingPath("$.totalPages").isEqualTo(2);
                });
    }

    @Test
    void rejectsOversizedPages() {
        assertThat(mvc.get().uri("/api/v1/admin/usuarios?size=500").header("Authorization", bearerAdmin("root")))
                .hasStatus(400);
    }

    @Test
    void changesRole() {
        when(administrarUsuarios.cambiarRol(Actor.admin("root"), "ana", Rol.ADMIN)).thenReturn(ANA.conRol(Rol.ADMIN));

        assertThat(mvc.put().uri("/api/v1/admin/usuarios/ana/rol").header("Authorization", bearerAdmin("root"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ADMIN\"}"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.rol").isEqualTo("ADMIN");
    }

    @Test
    void rejectsUnknownRole() {
        assertThat(mvc.put().uri("/api/v1/admin/usuarios/ana/rol").header("Authorization", bearerAdmin("root"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"GOD\"}"))
                .hasStatus(400);
    }

    @Test
    void regularUsersAreForbidden() {
        assertThat(mvc.get().uri("/api/v1/admin/usuarios").header("Authorization", bearer("ana"))).hasStatus(403);
        verifyNoInteractions(administrarUsuarios);
    }
}
