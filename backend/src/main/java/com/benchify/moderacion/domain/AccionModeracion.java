package com.benchify.moderacion.domain;

public enum AccionModeracion {
    BANCO_APROBADO, BANCO_RECHAZADO, RESENA_ELIMINADA, RESENA_MANTENIDA, IMAGEN_APROBADA, IMAGEN_RECHAZADA,
    /** La edición propuesta por el autor de un banco publicado (el recurso es el banco). */
    EDICION_APROBADA, EDICION_RECHAZADA;

    public TipoRecurso recurso() {
        return switch (this) {
            case BANCO_APROBADO, BANCO_RECHAZADO, EDICION_APROBADA, EDICION_RECHAZADA -> TipoRecurso.BANCO;
            case RESENA_ELIMINADA, RESENA_MANTENIDA -> TipoRecurso.RESENA;
            case IMAGEN_APROBADA, IMAGEN_RECHAZADA -> TipoRecurso.IMAGEN;
        };
    }
}
