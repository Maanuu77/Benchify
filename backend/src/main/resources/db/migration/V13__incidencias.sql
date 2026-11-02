-- Avisos de los usuarios sobre el estado de un banco (roto, sucio, desaparecido…)
CREATE TABLE incidencias (
    id               UUID PRIMARY KEY,
    banco_id         UUID         NOT NULL REFERENCES bancos (id) ON DELETE CASCADE,
    usuario_id       VARCHAR(128) NOT NULL REFERENCES usuarios (id),
    tipo             VARCHAR(20)  NOT NULL,
    comentario       VARCHAR(500),
    estado           VARCHAR(20)  NOT NULL DEFAULT 'ABIERTA',
    fecha            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resuelta_por     VARCHAR(128) REFERENCES usuarios (id),
    fecha_resolucion TIMESTAMPTZ,
    CONSTRAINT ck_incidencias_tipo CHECK (tipo IN ('ROTO', 'SUCIO', 'DESAPARECIDO', 'OTRO')),
    CONSTRAINT ck_incidencias_estado CHECK (estado IN ('ABIERTA', 'RESUELTA', 'DESCARTADA'))
);

-- Cada usuario puede tener como mucho un aviso abierto por banco
CREATE UNIQUE INDEX uk_incidencias_abierta_usuario ON incidencias (banco_id, usuario_id) WHERE estado = 'ABIERTA';
CREATE INDEX idx_incidencias_abiertas ON incidencias (fecha) WHERE estado = 'ABIERTA';
CREATE INDEX idx_incidencias_usuario ON incidencias (usuario_id);
