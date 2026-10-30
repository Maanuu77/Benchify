package com.benchify.usuario.application.service;

import com.benchify.shared.domain.Actor;
import com.benchify.shared.domain.BusinessRuleException;
import com.benchify.shared.domain.ForbiddenException;
import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Rol;
import com.benchify.usuario.application.port.in.AdministrarUsuariosUseCase;
import com.benchify.usuario.application.port.in.PerfilUsuarioUseCase;
import com.benchify.usuario.application.port.in.SincronizarUsuarioUseCase;
import com.benchify.usuario.application.port.out.CacheRolesPort;
import com.benchify.usuario.application.port.out.EstadisticasUsuarioPort;
import com.benchify.usuario.application.port.out.ProveedorIdentidadPort;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import com.benchify.usuario.domain.EstadisticasUsuario;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import com.benchify.usuario.domain.UsuarioNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * El rol guardado en la base de datos es el que cuenta para autorizar. Firebase solo recibe una
 * copia como custom claim para el cliente, y siempre después de confirmar la transacción: si la
 * base de datos falla, Firebase no cambia.
 */
@Service
@Transactional
public class UsuarioService implements SincronizarUsuarioUseCase, PerfilUsuarioUseCase, AdministrarUsuariosUseCase {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarios;
    private final ProveedorIdentidadPort proveedorIdentidad;
    private final CacheRolesPort cacheRoles;
    private final EstadisticasUsuarioPort estadisticas;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarios, ProveedorIdentidadPort proveedorIdentidad,
            CacheRolesPort cacheRoles, EstadisticasUsuarioPort estadisticas, Clock clock) {
        this.usuarios = usuarios;
        this.proveedorIdentidad = proveedorIdentidad;
        this.cacheRoles = cacheRoles;
        this.estadisticas = estadisticas;
        this.clock = clock;
    }

    @Override
    public Usuario sincronizar(IdentidadUsuario identidad) {
        Instant ahora = clock.instant();
        Optional<Usuario> guardado = usuarios.findById(identidad.id());
        Usuario usuario;
        if (guardado.isEmpty()) {
            // Alta: el único momento en que se toma el rol del token
            usuario = usuarios.registrarSiNoExiste(Usuario.registrar(identidad, ahora));
        } else if (guardado.get().debeSincronizarse(identidad, ahora)) {
            usuario = guardado.get().sincronizar(identidad, ahora);
            usuarios.actualizarSincronizacion(usuario);
        } else {
            usuario = guardado.get();
        }
        return usuario.rol() == identidad.rol() ? usuario : conciliarRol(usuario, identidad.rol());
    }

    /**
     * El token trae un rol distinto del guardado (es anterior a un cambio de rol o Firebase no se
     * pudo actualizar): manda la base de datos y se corrige el custom claim. La excepción es el
     * primer administrador, al que se da de alta poniéndole el claim a mano mientras no haya
     * ninguno en la base de datos.
     */
    private Usuario conciliarRol(Usuario usuario, Rol rolDelToken) {
        if (rolDelToken == Rol.ADMIN && !usuarios.existeAdministrador()) {
            log.info("User {} becomes the first administrator", usuario.id());
            usuarios.actualizarRol(usuario.id(), Rol.ADMIN);
            despuesDelCommit(() -> cacheRoles.invalidar(usuario.id()));
            return usuario.conRol(Rol.ADMIN);
        }
        despuesDelCommit(() -> enProveedorIdentidad(usuario.id(),
                () -> proveedorIdentidad.corregirRol(usuario.id(), usuario.rol())));
        return usuario;
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtener(String usuarioId) {
        return usuarios.findById(usuarioId).orElseThrow(() -> new UsuarioNotFoundException(usuarioId));
    }

    @Override
    public Usuario actualizarPerfil(Actor actor, ActualizarPerfilCommand command) {
        Usuario actualizado = obtener(actor.userId()).actualizarPerfil(command.nombre(), command.ciudad());
        usuarios.actualizarPerfil(actualizado);
        return actualizado;
    }

    @Override
    @Transactional(readOnly = true)
    public EstadisticasUsuario estadisticas(String usuarioId) {
        if (!usuarios.existsById(usuarioId)) {
            throw new UsuarioNotFoundException(usuarioId);
        }
        return estadisticas.contar(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Usuario> listar(Actor actor, PageQuery page) {
        exigirAdmin(actor);
        return usuarios.findAll(page);
    }

    @Override
    public Usuario cambiarRol(Actor actor, String usuarioId, Rol rol) {
        exigirAdmin(actor);
        if (actor.userId().equals(usuarioId) && rol != Rol.ADMIN) {
            // Evita que el último administrador se quede fuera del panel por error
            throw new BusinessRuleException("USUARIO_AUTODEGRADACION",
                    "No puedes retirarte a ti mismo el rol de administrador");
        }
        Usuario usuario = obtener(usuarioId);
        if (usuario.rol() == rol) {
            return usuario;
        }
        usuarios.actualizarRol(usuarioId, rol);
        log.info("Role of user {} changed to {} by {}", usuarioId, rol, actor.userId());
        despuesDelCommit(() -> {
            // En esta instancia se aplica ya; en las demás, cuando caduque su caché
            cacheRoles.invalidar(usuarioId);
            enProveedorIdentidad(usuarioId, () -> proveedorIdentidad.asignarRol(usuarioId, rol));
            // Sus sesiones abiertas no podrán renovar el ID token: al volver a entrar llevará el rol nuevo
            enProveedorIdentidad(usuarioId, () -> proveedorIdentidad.revocarSesiones(usuarioId));
        });
        return usuario.conRol(rol);
    }

    private static void exigirAdmin(Actor actor) {
        if (!actor.isAdmin()) {
            throw new ForbiddenException("ADMIN_REQUIRED", "Solo un administrador puede gestionar usuarios");
        }
    }

    /**
     * El cambio ya está confirmado en la base de datos, que es la que autoriza: un fallo de
     * Firebase no lo deshace. El claim se corrige en la siguiente sincronización del usuario.
     */
    private static void enProveedorIdentidad(String usuarioId, Runnable llamada) {
        try {
            llamada.run();
        } catch (RuntimeException e) {
            log.warn("Could not update user {} in the identity provider", usuarioId, e);
        }
    }

    /** Ejecuta la acción cuando se confirme la transacción en curso; si se deshace, no se ejecuta. */
    private static void despuesDelCommit(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accion.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }
}
