package com.benchify.usuario.application.port.out;

import com.benchify.shared.domain.PageQuery;
import com.benchify.shared.domain.PageResult;
import com.benchify.shared.domain.Rol;
import com.benchify.usuario.domain.Usuario;

import java.util.Optional;

/**
 * Cada actualización escribe solo sus columnas: una sincronización o un cambio de perfil que se
 * cruce con un cambio de rol no puede devolver el rol a su valor anterior.
 */
public interface UsuarioRepository {

    Optional<Usuario> findById(String id);

    boolean existsById(String id);

    long count();

    /**
     * Alta idempotente: si otra petición simultánea acaba de dar de alta al mismo usuario no
     * falla, y devuelve la fila guardada, sea la recibida o la que ya existía.
     */
    Usuario registrarSiNoExiste(Usuario usuario);

    /** Guarda los datos que aporta el proveedor de identidad (email y avatar) y el último acceso. */
    void actualizarSincronizacion(Usuario usuario);

    /** Guarda el nombre y la ciudad. */
    void actualizarPerfil(Usuario usuario);

    void actualizarRol(String usuarioId, Rol rol);

    /** {@code true} si algún usuario tiene ya el rol de administrador. */
    boolean existeAdministrador();

    /** Usuarios ordenados del más reciente al más antiguo. */
    PageResult<Usuario> findAll(PageQuery page);
}
