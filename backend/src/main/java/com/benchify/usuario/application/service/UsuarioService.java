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
import com.benchify.usuario.application.port.out.ProveedorIdentidadPort;
import com.benchify.usuario.application.port.out.UsuarioRepository;
import com.benchify.usuario.domain.IdentidadUsuario;
import com.benchify.usuario.domain.Usuario;
import com.benchify.usuario.domain.UsuarioNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class UsuarioService implements SincronizarUsuarioUseCase, PerfilUsuarioUseCase, AdministrarUsuariosUseCase {

    private final UsuarioRepository usuarios;
    private final ProveedorIdentidadPort proveedorIdentidad;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarios, ProveedorIdentidadPort proveedorIdentidad, Clock clock) {
        this.usuarios = usuarios;
        this.proveedorIdentidad = proveedorIdentidad;
        this.clock = clock;
    }

    @Override
    public Usuario sincronizar(IdentidadUsuario identidad) {
        return usuarios.findById(identidad.id())
                .map(existente -> existente.difiereDe(identidad)
                        ? usuarios.save(existente.sincronizar(identidad, clock.instant()))
                        : existente)
                .orElseGet(() -> usuarios.save(Usuario.registrar(identidad, clock.instant())));
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtener(String usuarioId) {
        return usuarios.findById(usuarioId).orElseThrow(() -> new UsuarioNotFoundException(usuarioId));
    }

    @Override
    public Usuario actualizarPerfil(Actor actor, ActualizarPerfilCommand command) {
        Usuario usuario = obtener(actor.userId());
        return usuarios.save(usuario.actualizarPerfil(command.nombre(), command.ciudad()));
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
        proveedorIdentidad.asignarRol(usuarioId, rol);
        return usuarios.save(usuario.conRol(rol));
    }

    private static void exigirAdmin(Actor actor) {
        if (!actor.isAdmin()) {
            throw new ForbiddenException("ADMIN_REQUIRED", "Solo un administrador puede gestionar usuarios");
        }
    }
}
