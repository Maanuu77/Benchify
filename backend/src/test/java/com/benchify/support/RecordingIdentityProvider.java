package com.benchify.support;

import com.benchify.shared.domain.Rol;
import com.benchify.usuario.application.port.out.ProveedorIdentidadPort;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Sustituye a Firebase en los tests y recuerda, por usuario, las llamadas que recibe. */
public class RecordingIdentityProvider implements ProveedorIdentidadPort {

    private final Map<String, List<String>> llamadas = new ConcurrentHashMap<>();

    @Override
    public void asignarRol(String usuarioId, Rol rol) {
        registrar(usuarioId, "asignarRol:" + rol);
    }

    @Override
    public void corregirRol(String usuarioId, Rol rol) {
        registrar(usuarioId, "corregirRol:" + rol);
    }

    @Override
    public void revocarSesiones(String usuarioId) {
        registrar(usuarioId, "revocarSesiones");
    }

    /** Llamadas recibidas para el usuario, en orden, como {@code asignarRol:ADMIN} o {@code revocarSesiones}. */
    public List<String> llamadas(String usuarioId) {
        return List.copyOf(llamadas.getOrDefault(usuarioId, List.of()));
    }

    private void registrar(String usuarioId, String llamada) {
        llamadas.computeIfAbsent(usuarioId, id -> new CopyOnWriteArrayList<>()).add(llamada);
    }
}
