package com.benchify.usuario.infrastructure.web;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.Rol;
import com.benchify.support.ApiControllerTest;
import com.benchify.usuario.application.port.in.PerfilUsuarioUseCase;
import com.benchify.usuario.application.port.in.PerfilUsuarioUseCase.ActualizarPerfilCommand;
import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import com.benchify.usuario.domain.UsuarioNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;

import static com.benchify.support.StubIdTokenVerifier.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiControllerTest(UsuarioController.class)
@Import(UsuarioWebMapperImpl.class)
class UsuarioControllerTest {

    private static final Usuario ANA = Usuario.registrar(
            new IdentidadUsuario("ana", "ana@x.com", "Ana", null, Rol.USER), Instant.parse("2026-01-01T00:00:00Z"));

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private PerfilUsuarioUseCase perfilUsuario;
    @Autowired
    private SincronizarUsuarioUseCase sincronizarUsuario;

    @Test
    void returnsOwnProfileAndSyncsTheUserFirst() {
        // uid propio: el interceptor recuerda usuarios ya sincronizados dentro del contexto compartido
        Usuario nuevo = Usuario.registrar(new IdentidadUsuario("nuevo", "nuevo@x.com", "Nuevo", null, Rol.USER),
                Instant.parse("2026-01-01T00:00:00Z"));
        when(perfilUsuario.obtener("nuevo")).thenReturn(nuevo);

        assertThat(mvc.get().uri("/api/v1/usuarios/me").header("Authorization", bearer("nuevo")))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.id").isEqualTo("nuevo");
                    assertThat(json).extractingPath("$.email").isEqualTo("nuevo@x.com");
                    assertThat(json).extractingPath("$.rol").isEqualTo("USER");
                });
        verify(sincronizarUsuario).sincronizar(
                new IdentidadUsuario("nuevo", "nuevo@benchify.test", "Usuario nuevo", null, Rol.USER));
    }

    @Test
    void ownProfileRequiresAuthentication() {
        assertThat(mvc.get().uri("/api/v1/usuarios/me")).hasStatus(401);
        verifyNoInteractions(perfilUsuario);
    }

    @Test
    void updatesOwnProfile() {
        when(perfilUsuario.actualizarPerfil(Actor.user("ana"), new ActualizarPerfilCommand("Ana B.", "Triana")))
                .thenReturn(ANA.actualizarPerfil("Ana B.", "Triana"));

        assertThat(mvc.patch().uri("/api/v1/usuarios/me").header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana B.\",\"ciudad\":\"Triana\"}"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.ciudad").isEqualTo("Triana");
    }

    @Test
    void rejectsBlankName() {
        assertThat(mvc.patch().uri("/api/v1/usuarios/me").header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\" \"}"))
                .hasStatus(400)
                .bodyJson().extractingPath("$.errors[0].field").isEqualTo("nombre");
    }

    @Test
    void publicProfileHidesEmailAndRole() {
        when(perfilUsuario.obtener("ana")).thenReturn(ANA);

        assertThat(mvc.get().uri("/api/v1/usuarios/ana"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.nombre").isEqualTo("Ana");
                    assertThat(json).doesNotHavePath("$.email");
                    assertThat(json).doesNotHavePath("$.rol");
                });
    }

    @Test
    void unknownUserIs404() {
        when(perfilUsuario.obtener("ghost")).thenThrow(new UsuarioNotFoundException("ghost"));

        assertThat(mvc.get().uri("/api/v1/usuarios/ghost"))
                .hasStatus(404)
                .bodyJson().extractingPath("$.code").isEqualTo("USUARIO_NOT_FOUND");
    }
}
