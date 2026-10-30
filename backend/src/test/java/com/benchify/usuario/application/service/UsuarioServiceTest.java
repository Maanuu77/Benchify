package com.benchify.usuario.application.service;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Rol;
import com.benchify.usuario.application.port.in.PerfilUsuarioUseCase.ActualizarPerfilCommand;
import com.benchify.usuario.application.port.out.CacheRolesPort;
import com.benchify.usuario.application.port.out.EstadisticasUsuarioPort;
import com.benchify.usuario.application.port.out.ProveedorIdentidadPort;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import com.benchify.usuario.domain.EstadisticasUsuario;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import com.benchify.usuario.domain.UsuarioNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
    private CacheRolesPort cacheRoles;
    @Mock
    private EstadisticasUsuarioPort estadisticas;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarios, proveedorIdentidad, cacheRoles, estadisticas,
                Clock.fixed(AHORA, ZoneOffset.UTC));
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
    void newUserTakesTheRoleOfTheTokenSoTheFirstAdminCanBeBootstrapped() {
        when(usuarios.findById("root")).thenReturn(Optional.empty());

        Usuario usuario = service.sincronizar(new IdentidadUsuario("root", "r@x.com", "Root", null, Rol.ADMIN));

        assertThat(usuario.rol()).isEqualTo(Rol.ADMIN);
        verifyNoInteractions(proveedorIdentidad);
    }

    @Test
    void firstSyncReturnsTheRowStoredByAConcurrentRequest() {
        // Otra petición del mismo usuario lo ha dado de alta entre la consulta y el INSERT
        Usuario guardado = usuario("uid", Rol.USER);
        when(usuarios.findById("uid")).thenReturn(Optional.empty());
        when(usuarios.registrarSiNoExiste(any())).thenReturn(guardado);

        assertThat(service.sincronizar(new IdentidadUsuario("uid", "uid@x.com", "Ana", null, Rol.USER)))
                .isSameAs(guardado);
        verify(usuarios, never()).actualizarSincronizacion(any());
    }

    @Test
    void doesNotWriteWhenIdentityIsUnchangedAndLastAccessIsRecent() {
        IdentidadUsuario identidad = new IdentidadUsuario("uid", "a@x.com", "Ana", null, Rol.USER);
        Usuario existente = Usuario.registrar(identidad, AHORA.minus(Usuario.REFRESCO_ULTIMO_ACCESO));
        when(usuarios.findById("uid")).thenReturn(Optional.of(existente));

        assertThat(service.sincronizar(identidad)).isSameAs(existente);
        verify(usuarios, never()).actualizarSincronizacion(any());
    }

    @Test
    void refreshesLastAccessWhenTheStoredOneIsOlderThanFifteenMinutes() {
        IdentidadUsuario identidad = new IdentidadUsuario("uid", "a@x.com", "Ana", null, Rol.USER);
        Usuario existente = Usuario.registrar(identidad, AHORA.minus(Usuario.REFRESCO_ULTIMO_ACCESO).minusSeconds(1));
        when(usuarios.findById("uid")).thenReturn(Optional.of(existente));

        Usuario sincronizado = service.sincronizar(identidad);

        assertThat(sincronizado.ultimoAcceso()).isEqualTo(AHORA);
        verify(usuarios).actualizarSincronizacion(sincronizado);
    }

    @Test
    void staleAdminTokenCannotRewriteTheStoredRole() {
        when(usuarios.findById("ana")).thenReturn(Optional.of(usuario("ana", Rol.USER)));
        when(usuarios.existeAdministrador()).thenReturn(true);

        Usuario sincronizado = service.sincronizar(new IdentidadUsuario("ana", "ana@x.com", "x", null, Rol.ADMIN));

        assertThat(sincronizado.rol()).isEqualTo(Rol.USER);
        verify(usuarios, never()).actualizarRol(anyString(), any());
        // El claim de Firebase se corrige para que el cliente deje de mostrarle el panel
        verify(proveedorIdentidad).corregirRol("ana", Rol.USER);
    }

    @Test
    void storedAdminKeepsTheRoleWhenTheTokenSaysUser() {
        when(usuarios.findById("root")).thenReturn(Optional.of(usuario("root", Rol.ADMIN)));

        Usuario sincronizado = service.sincronizar(new IdentidadUsuario("root", "root@x.com", "x", null, Rol.USER));

        assertThat(sincronizado.rol()).isEqualTo(Rol.ADMIN);
        verify(usuarios, never()).existeAdministrador();
        verify(proveedorIdentidad).corregirRol("root", Rol.ADMIN);
    }

    @Test
    void existingUserBecomesTheFirstAdminWhileThereIsNone() {
        when(usuarios.findById("root")).thenReturn(Optional.of(usuario("root", Rol.USER)));
        when(usuarios.existeAdministrador()).thenReturn(false);

        Usuario sincronizado = service.sincronizar(new IdentidadUsuario("root", "root@x.com", "x", null, Rol.ADMIN));

        assertThat(sincronizado.rol()).isEqualTo(Rol.ADMIN);
        verify(usuarios).actualizarRol("root", Rol.ADMIN);
        verify(cacheRoles).invalidar("root");
        verifyNoInteractions(proveedorIdentidad);
    }

    @Test
    void identityProviderFailuresDoNotBreakTheSync() {
        when(usuarios.findById("ana")).thenReturn(Optional.of(usuario("ana", Rol.USER)));
        when(usuarios.existeAdministrador()).thenReturn(true);
        doThrow(new IllegalStateException("Firebase down")).when(proveedorIdentidad).corregirRol("ana", Rol.USER);

        assertThat(service.sincronizar(new IdentidadUsuario("ana", "ana@x.com", "x", null, Rol.ADMIN)).rol())
                .isEqualTo(Rol.USER);
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
    void updatesOwnProfileWithoutTouchingTheRole() {
        when(usuarios.findById("uid")).thenReturn(Optional.of(usuario("uid", Rol.USER)));

        Usuario actualizado = service.actualizarPerfil(Actor.user("uid"), new ActualizarPerfilCommand("Ana", "Triana"));

        assertThat(actualizado.nombre()).isEqualTo("Ana");
        assertThat(actualizado.ciudad()).isEqualTo("Triana");
        verify(usuarios).actualizarPerfil(actualizado);
        verify(usuarios, never()).actualizarRol(anyString(), any());
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
    void adminChangesTheRoleInTheDatabaseAndThenInTheIdentityProvider() {
        when(usuarios.findById("ana")).thenReturn(Optional.of(usuario("ana", Rol.ADMIN)));

        Usuario degradada = service.cambiarRol(Actor.admin("root"), "ana", Rol.USER);

        assertThat(degradada.rol()).isEqualTo(Rol.USER);
        InOrder orden = inOrder(usuarios, cacheRoles, proveedorIdentidad);
        orden.verify(usuarios).actualizarRol("ana", Rol.USER);
        orden.verify(cacheRoles).invalidar("ana");
        orden.verify(proveedorIdentidad).asignarRol("ana", Rol.USER);
        orden.verify(proveedorIdentidad).revocarSesiones("ana");
    }

    @Test
    void identityProviderFailuresAfterTheRoleChangeAreNotPropagated() {
        when(usuarios.findById("ana")).thenReturn(Optional.of(usuario("ana", Rol.USER)));
        doThrow(new IllegalStateException("Firebase down")).when(proveedorIdentidad).asignarRol("ana", Rol.ADMIN);

        assertThat(service.cambiarRol(Actor.admin("root"), "ana", Rol.ADMIN).rol()).isEqualTo(Rol.ADMIN);
        // La base de datos ya ha cambiado: se revocan las sesiones igualmente y el claim se corrige al sincronizar
        verify(cacheRoles).invalidar("ana");
        verify(proveedorIdentidad).revocarSesiones("ana");
    }

    @Test
    void sameRoleIsANoOp() {
        Usuario ana = usuario("ana", Rol.ADMIN);
        when(usuarios.findById("ana")).thenReturn(Optional.of(ana));

        assertThat(service.cambiarRol(Actor.admin("root"), "ana", Rol.ADMIN)).isSameAs(ana);
        verify(usuarios, never()).actualizarRol(anyString(), any());
        verifyNoInteractions(proveedorIdentidad, cacheRoles);
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

    /** Con una transacción en curso, Firebase y la caché esperan a que se confirme. */
    @Nested
    class WithinATransaction {

        @BeforeEach
        void beginTransaction() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @AfterEach
        void endTransaction() {
            TransactionSynchronizationManager.clearSynchronization();
        }

        @Test
        void roleChangeReachesFirebaseOnlyAfterCommit() {
            when(usuarios.findById("ana")).thenReturn(Optional.of(usuario("ana", Rol.USER)));

            service.cambiarRol(Actor.admin("root"), "ana", Rol.ADMIN);

            verify(usuarios).actualizarRol("ana", Rol.ADMIN);
            verifyNoInteractions(proveedorIdentidad, cacheRoles);

            TransactionSynchronizationUtils.triggerAfterCommit();

            verify(cacheRoles).invalidar("ana");
            verify(proveedorIdentidad).asignarRol("ana", Rol.ADMIN);
            verify(proveedorIdentidad).revocarSesiones("ana");
        }

        @Test
        void rolledBackRoleChangeNeverReachesFirebase() {
            when(usuarios.findById("ana")).thenReturn(Optional.of(usuario("ana", Rol.USER)));

            service.cambiarRol(Actor.admin("root"), "ana", Rol.ADMIN);
            TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            verifyNoInteractions(proveedorIdentidad, cacheRoles);
        }

        @Test
        void claimCorrectionWaitsForTheCommitToo() {
            when(usuarios.findById("ana")).thenReturn(Optional.of(usuario("ana", Rol.USER)));
            when(usuarios.existeAdministrador()).thenReturn(true);

            service.sincronizar(new IdentidadUsuario("ana", "ana@x.com", "x", null, Rol.ADMIN));
            verifyNoInteractions(proveedorIdentidad);

            TransactionSynchronizationUtils.triggerAfterCommit();
            verify(proveedorIdentidad).corregirRol("ana", Rol.USER);
        }
    }

    private static Usuario usuario(String id, Rol rol) {
        return Usuario.registrar(new IdentidadUsuario(id, id + "@x.com", id, null, rol), AHORA.minusSeconds(60));
    }
}
