-- Ediciones de bancos publicados propuestas por su autor. El banco sigue publicado con los datos
-- anteriores hasta que un moderador aprueba la edición, que entonces se aplica.
CREATE TABLE revisiones_banco (
    id             UUID PRIMARY KEY,
    banco_id       UUID         NOT NULL REFERENCES bancos (id) ON DELETE CASCADE,
    autor_id       VARCHAR(128) NOT NULL REFERENCES usuarios (id),
    -- DatosBanco propuestos: título, descripción, ubicación, dirección, características y etiquetas
    datos          JSONB        NOT NULL,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    fecha          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    revisada_por   VARCHAR(128) REFERENCES usuarios (id),
    fecha_revision TIMESTAMPTZ,
    CONSTRAINT ck_revisiones_banco_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA'))
);

-- Como mucho una edición pendiente por banco: una nueva sustituye a la anterior
CREATE UNIQUE INDEX uk_revisiones_banco_pendiente ON revisiones_banco (banco_id) WHERE estado = 'PENDIENTE';
CREATE INDEX idx_revisiones_banco_pendientes ON revisiones_banco (fecha, id) WHERE estado = 'PENDIENTE';
CREATE INDEX idx_revisiones_banco_autor ON revisiones_banco (autor_id);
