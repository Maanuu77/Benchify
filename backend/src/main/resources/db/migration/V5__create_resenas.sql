-- Reseñas: una por usuario y banco. La media y el total se desnormalizan en bancos.
CREATE TABLE resenas (
    id            UUID PRIMARY KEY,
    banco_id      UUID          NOT NULL REFERENCES bancos (id) ON DELETE CASCADE,
    usuario_id    VARCHAR(128)  NOT NULL REFERENCES usuarios (id),
    puntuacion    INTEGER       NOT NULL,
    comentario    VARCHAR(1000),
    fecha         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_edicion TIMESTAMPTZ,
    num_reportes  INTEGER       NOT NULL DEFAULT 0,
    reportada     BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_resenas_banco_usuario UNIQUE (banco_id, usuario_id),
    CONSTRAINT ck_resenas_puntuacion CHECK (puntuacion BETWEEN 1 AND 5),
    CONSTRAINT ck_resenas_reportes CHECK (num_reportes >= 0)
);

CREATE INDEX idx_resenas_banco_fecha ON resenas (banco_id, fecha DESC);
CREATE INDEX idx_resenas_usuario_fecha ON resenas (usuario_id, fecha DESC);
-- Cola de moderación: solo las reseñas reportadas, las más reportadas primero
CREATE INDEX idx_resenas_reportadas ON resenas (num_reportes DESC, fecha) WHERE reportada;

-- Un usuario solo puede reportar una vez cada reseña
CREATE TABLE resena_reportes (
    resena_id  UUID         NOT NULL REFERENCES resenas (id) ON DELETE CASCADE,
    usuario_id VARCHAR(128) NOT NULL REFERENCES usuarios (id),
    fecha      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (resena_id, usuario_id)
);
