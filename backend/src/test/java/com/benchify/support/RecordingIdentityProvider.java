package com.benchify.support;

import com.benchify.shared.domain.Rol;
import com.benchify.usuario.application.port.out.ProveedorIdentidadPort;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sustituye a Firebase en los tests y recuerda los roles asignados. */
public class RecordingIdentityProvider implements ProveedorIdentidadPort {

    private final Map<String, Rol> roles = new ConcurrentHashMap<>();

    @Override
    public void asignarRol(String usuarioId, Rol rol) {
        roles.put(usuarioId, rol);
    }

    public Rol rolAsignado(String usuarioId) {
        return roles.get(usuarioId);
    }
}
