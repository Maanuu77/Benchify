CREATE TABLE favoritos (
    usuario_id VARCHAR(128) NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    banco_id   UUID         NOT NULL REFERENCES bancos (id) ON DELETE CASCADE,
    fecha      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, banco_id)
);

CREATE INDEX idx_favoritos_usuario_fecha ON favoritos (usuario_id, fecha DESC);
CREATE INDEX idx_favoritos_banco ON favoritos (banco_id);
