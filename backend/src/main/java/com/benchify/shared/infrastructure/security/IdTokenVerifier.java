package com.benchify.shared.infrastructure.security;

/**
 * Valida un ID token emitido por el proveedor de identidad. En producción lo implementa
 * Firebase; en los tests se sustituye por un verificador simulado.
 */
public interface IdTokenVerifier {

    /**
     * @throws InvalidIdTokenException si el token está caducado, mal firmado o no pertenece al proyecto
     */
    AuthenticatedUser verify(String idToken);
}
