package com.benchify.shared.domain;

/** Rol de un usuario. Se guarda en Firebase como custom claim {@code role}. */
public enum Rol {
    USER,
    ADMIN;

    /** Rol a partir del valor del claim; cualquier valor desconocido o ausente es {@link #USER}. */
    public static Rol fromClaim(Object claim) {
        if (claim instanceof String value) {
            for (Rol rol : values()) {
                if (rol.name().equalsIgnoreCase(value.trim())) {
                    return rol;
                }
            }
        }
        return USER;
    }
}
