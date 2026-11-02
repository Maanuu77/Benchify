package com.benchify.moderacion.domain;

public enum AccionModeracion {
    BANCO_APROBADO, BANCO_RECHAZADO, RESENA_ELIMINADA, RESENA_MANTENIDA, IMAGEN_APROBADA, IMAGEN_RECHAZADA;

    public TipoRecurso recurso() {
        return switch (this) {
            case BANCO_APROBADO, BANCO_RECHAZADO -> TipoRecurso.BANCO;
            case RESENA_ELIMINADA, RESENA_MANTENIDA -> TipoRecurso.RESENA;
            case IMAGEN_APROBADA, IMAGEN_RECHAZADA -> TipoRecurso.IMAGEN;
        };
    }
}
