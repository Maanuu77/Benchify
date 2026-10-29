package com.benchify.usuario.application.service;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Rol;
import com.benchify.usuario.application.port.in.PerfilUsuarioUseCase.ActualizarPerfilCommand;
import com.benchify.usuario.application.port.out.EstadisticasUsuarioPort;
import com.benchify.usuario.application.port.out.ProveedorIdentidadPort;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import com.benchify.usuario.domain.EstadisticasUsuario;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import com.benchify.usuario.domain.UsuarioNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T10:00:00Z");

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private ProveedorIdentidadPort proveedorIdentidad;
    @Mock
    private EstadisticasUsuarioPort estadisticas;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarios, proveedorIdentidad, estadisticas, Clock.fixed(AHORA, ZoneOffset.UTC));
        lenient().when(usuarios.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(usuarios.registrarSiNoExiste(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registersUnknownUserOnFirstSync() {
        when(usuarios.findById("uid")).thenReturn(Optional.empty());

        Usuario usuario = service.sincronizar(new IdentidadUsuario("uid", "a@x.com", "Ana", null, Rol.USER));

        assertThat(usuario.fechaRegistro()).isEqualTo(AHORA);
        verify(usuarios).registrarSiNoExiste(usuario);
    }

    @Test
    void firstSyncReturnsTheRowStoredByAConcurrentRequest() {
        // Otra petición del mismo usuario lo ha dado de alta entre la consulta y el INSERT
        Usuario guardado = usuario("uid", Rol.USER);
        when(usuarios.findById("uid")).thenReturn(Optional.empty());
        when(usuarios.registrarSiNoExiste(any())).thenReturn(guardado);

        assertThat(service.sincronizar(new IdentidadUsuario("uid", "a@x.com", "Ana", null, Rol.USER)))
                .isSameAs(guardado);
        verify(usuarios, never()).save(any());
    }

    @Test
    void doesNotWriteWhenIdentityIsUnchanged() {
        IdentidadUsuario identidad = new IdentidadUsuario("uid", "a@x.com", "Ana", null, Rol.USER);
        Usuario existente = Usuario.registrar(identidad, AHORA.minusSeconds(3600));
        when(usuarios.findById("uid")).thenReturn(Optional.of(existente));

        assertThat(service.sincronizar(identidad)).isSameAs(existente);
        verify(usuarios, never()).save(any());
    }

    @Test
    void persistsRoleChangesComingFromTheToken() {
        Usuario existente = usuario("uid", Rol.USER);
        when(usuarios.findById("uid")).thenReturn(Optional.of(existente));

        Usuario sincronizado = service.sincronizar(new IdentidadUsuario("uid", "uid@x.com", "x", null, Rol.ADMIN));

        assertThat(sincronizado.rol()).isEqualTo(Rol.ADMIN);
        verify(usuarios).save(sincronizado);
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        when(usuarios.findById("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener("nadie")).isInstanceOf(UsuarioNotFoundException.class);
    }

    @Test
    void statisticsRequireAnExistingUser() {
        when(usuarios.existsById("ana")).thenReturn(true);
        when(estadisticas.contar("ana")).thenReturn(new EstadisticasUsuario(2, 5, 3, 7));

        assertThat(service.estadisticas("ana")).isEqualTo(new EstadisticasUsuario(2, 5, 3, 7));
        assertThatThrownBy(() -> service.estadisticas("ghost")).isInstanceOf(UsuarioNotFoundException.class);
    }

    @Test
    void updatesOwnProfile() {
        when(usuarios.findById("uid")).thenReturn(Optional.of(usuario("uid", Rol.USER)));

        Usuario actualizado = service.actualizarPerfil(Actor.user("uid"), new ActualizarPerfilCommand("Ana", "Triana"));

        assertThat(actualizado.nombre()).isEqualTo("Ana");
        assertThat(actualizado.ciudad()).isEqualTo("Triana");
    }

    @Test
    void onlyAdminsCanListUsers() {
        assertThatThrownBy(() -> service.listar(Actor.user("uid"), PageQuery.first(10)))
                .isInstanceOf(ForbiddenException.class);

        PageResult<Usuario> page = new PageResult<>(List.of(usuario("a", Rol.USER)), 0, 10, 1);
        when(usuarios.findAll(PageQuery.first(10))).thenReturn(page);
        assertThat(service.listar(Actor.admin("root"), PageQuery.first(10))).isEqualTo(page);
    }

    @Test
    void adminPromotesUserInIdentityProviderAndLocally() {
        when(usuarios.findById("ana")).thenReturn(Optional.of(usuario("ana", Rol.USER)));

        Usuario promovido = service.cambiarRol(Actor.admin("root"), "ana", Rol.ADMIN);

        assertThat(promovido.rol()).isEqualTo(Rol.ADMIN);
        verify(proveedorIdentidad).asignarRol("ana", Rol.ADMIN);
        verify(usuarios).save(promovido);
    }

    @Test
    void sameRoleIsANoOp() {
        Usuario ana = usuario("ana", Rol.ADMIN);
        when(usuarios.findById("ana")).thenReturn(Optional.of(ana));

        assertThat(service.cambiarRol(Actor.admin("root"), "ana", Rol.ADMIN)).isSameAs(ana);
        verifyNoInteractions(proveedorIdentidad);
    }

    @Test
    void regularUsersCannotChangeRoles() {
        assertThatThrownBy(() -> service.cambiarRol(Actor.user("ana"), "ana", Rol.ADMIN))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(proveedorIdentidad);
    }

    @Test
    void adminCannotDemoteThemselves() {
        assertThatThrownBy(() -> service.cambiarRol(Actor.admin("root"), "root", Rol.USER))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("USUARIO_AUTODEGRADACION");
        verifyNoInteractions(proveedorIdentidad);
    }

    private static Usuario usuario(String id, Rol rol) {
        return Usuario.registrar(new IdentidadUsuario(id, id + "@x.com", id, null, rol), AHORA.minusSeconds(60));
    }
}
