-- Fotos de bancos. El fichero vive en Firebase Cloud Storage; aquí solo su ruta y su URL pública.
CREATE TABLE imagenes (
    id           UUID PRIMARY KEY,
    banco_id     UUID          NOT NULL REFERENCES bancos (id) ON DELETE CASCADE,
    ruta         VARCHAR(512)  NOT NULL,
    url          VARCHAR(1024) NOT NULL,
    content_type VARCHAR(50)   NOT NULL,
    tamano_bytes BIGINT        NOT NULL,
    subida_por   VARCHAR(128)  NOT NULL REFERENCES usuarios (id),
    estado       VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    fecha_subida TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_imagenes_ruta UNIQUE (ruta),
    CONSTRAINT ck_imagenes_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT ck_imagenes_tamano CHECK (tamano_bytes > 0)
);

CREATE INDEX idx_imagenes_banco_estado ON imagenes (banco_id, estado, fecha_subida);
CREATE INDEX idx_imagenes_estado_fecha ON imagenes (estado, fecha_subida);
