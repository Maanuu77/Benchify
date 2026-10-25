package com.benchify.usuario.application.port.out;

import com.benchify.shared.domain.Rol;

/** Operaciones sobre el proveedor de identidad externo (Firebase Authentication). */
public interface ProveedorIdentidadPort {

    void asignarRol(String usuarioId, Rol rol);
}
