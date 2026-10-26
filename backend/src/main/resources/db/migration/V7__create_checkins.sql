-- Visitas a bancos. Se guarda dónde estaba el usuario para poder auditar la distancia.
CREATE TABLE checkins (
    id               UUID PRIMARY KEY,
    usuario_id       VARCHAR(128)     NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    banco_id         UUID             NOT NULL REFERENCES bancos (id) ON DELETE CASCADE,
    lat              DOUBLE PRECISION NOT NULL,
    lng              DOUBLE PRECISION NOT NULL,
    distancia_metros DOUBLE PRECISION NOT NULL,
    fecha            TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT ck_checkins_lat CHECK (lat BETWEEN -90 AND 90),
    CONSTRAINT ck_checkins_lng CHECK (lng BETWEEN -180 AND 180),
    CONSTRAINT ck_checkins_distancia CHECK (distancia_metros >= 0)
);

CREATE INDEX idx_checkins_usuario_fecha ON checkins (usuario_id, fecha DESC);
CREATE INDEX idx_checkins_usuario_banco_fecha ON checkins (usuario_id, banco_id, fecha DESC);
CREATE INDEX idx_checkins_banco ON checkins (banco_id);
