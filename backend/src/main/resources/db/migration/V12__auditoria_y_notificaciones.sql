-- Registro de lo que hacen los moderadores: quién, qué, sobre qué y por qué
CREATE TABLE eventos_moderacion (
    id           UUID PRIMARY KEY,
    moderador_id VARCHAR(128) NOT NULL REFERENCES usuarios (id),
    accion       VARCHAR(40)  NOT NULL,
    recurso_tipo VARCHAR(20)  NOT NULL,
    recurso_id   UUID         NOT NULL,
    motivo       VARCHAR(500),
    fecha        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_eventos_recurso CHECK (recurso_tipo IN ('BANCO', 'IMAGEN', 'RESENA'))
);

CREATE INDEX idx_eventos_moderacion_fecha ON eventos_moderacion (fecha DESC);
CREATE INDEX idx_eventos_moderacion_recurso ON eventos_moderacion (recurso_tipo, recurso_id);

-- Avisos a los usuarios (por ahora, el resultado de la moderación de su contenido)
CREATE TABLE notificaciones (
    id         UUID PRIMARY KEY,
    usuario_id VARCHAR(128) NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    tipo       VARCHAR(40)  NOT NULL,
    titulo     VARCHAR(200) NOT NULL,
    mensaje    VARCHAR(500),
    enlace     VARCHAR(300),
    leida      BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notificaciones_usuario_fecha ON notificaciones (usuario_id, fecha DESC);
CREATE INDEX idx_notificaciones_no_leidas ON notificaciones (usuario_id) WHERE NOT leida;
