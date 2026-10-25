-- Catálogo de etiquetas (sustituye al antiguo data.sql)
CREATE TABLE etiquetas (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    color  VARCHAR(7)  NOT NULL,
    icono  VARCHAR(50) NOT NULL,
    CONSTRAINT uk_etiquetas_nombre UNIQUE (nombre),
    CONSTRAINT ck_etiquetas_color CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

INSERT INTO etiquetas (nombre, color, icono) VALUES
    ('romantico',  '#E74C3C', 'favorite'),
    ('relax',      '#3498DB', 'spa'),
    ('vistas',     '#2ECC71', 'landscape'),
    ('sombra',     '#7F8C8D', 'park'),
    ('centrico',   '#F39C12', 'place'),
    ('naturaleza', '#27AE60', 'nature'),
    ('accesible',  '#1ABC9C', 'accessible'),
    ('infantil',   '#E67E22', 'child_care'),
    ('historico',  '#8E44AD', 'account_balance');

CREATE TABLE bancos (
    id                 UUID PRIMARY KEY,
    titulo             VARCHAR(150)     NOT NULL,
    descripcion        VARCHAR(500),
    lat                DOUBLE PRECISION NOT NULL,
    lng                DOUBLE PRECISION NOT NULL,
    -- Mantenida por la base de datos a partir de lat/lng: la aplicación nunca la escribe
    ubicacion          geography(Point, 4326)
                       GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography) STORED,
    direccion          VARCHAR(255),
    ciudad             VARCHAR(100),
    provincia          VARCHAR(100),
    pais               VARCHAR(100)     NOT NULL DEFAULT 'España',
    estado             VARCHAR(20)      NOT NULL DEFAULT 'PENDIENTE',
    con_sombra         BOOLEAN          NOT NULL DEFAULT FALSE,
    accesible          BOOLEAN          NOT NULL DEFAULT FALSE,
    con_respaldo       BOOLEAN          NOT NULL DEFAULT FALSE,
    material           VARCHAR(20),
    puntuacion_media   DOUBLE PRECISION NOT NULL DEFAULT 0,
    num_resenas        INTEGER          NOT NULL DEFAULT 0,
    creado_por         VARCHAR(128)     NOT NULL REFERENCES usuarios (id),
    fecha_subida       TIMESTAMPTZ      NOT NULL DEFAULT now(),
    fecha_verificacion TIMESTAMPTZ,
    version            BIGINT           NOT NULL DEFAULT 0,
    CONSTRAINT ck_bancos_lat CHECK (lat BETWEEN -90 AND 90),
    CONSTRAINT ck_bancos_lng CHECK (lng BETWEEN -180 AND 180),
    CONSTRAINT ck_bancos_estado CHECK (estado IN ('PENDIENTE', 'PUBLICADO', 'OCULTO', 'ELIMINADO')),
    CONSTRAINT ck_bancos_material CHECK (material IN ('MADERA', 'METAL', 'PIEDRA', 'HORMIGON', 'PLASTICO', 'OTRO')),
    CONSTRAINT ck_bancos_puntuacion CHECK (puntuacion_media BETWEEN 0 AND 5 AND num_resenas >= 0)
);

-- Búsqueda por radio (ST_DWithin sobre geography, en metros)
CREATE INDEX idx_bancos_ubicacion ON bancos USING GIST (ubicacion);
-- Bounding box del mapa (rectángulo exacto en lat/lng sobre geometry)
CREATE INDEX idx_bancos_ubicacion_geom ON bancos USING GIST ((ubicacion::geometry));
CREATE INDEX idx_bancos_estado_fecha ON bancos (estado, fecha_subida);
CREATE INDEX idx_bancos_creado_por ON bancos (creado_por, fecha_subida DESC);

CREATE TABLE banco_etiquetas (
    banco_id    UUID   NOT NULL REFERENCES bancos (id) ON DELETE CASCADE,
    etiqueta_id BIGINT NOT NULL REFERENCES etiquetas (id),
    PRIMARY KEY (banco_id, etiqueta_id)
);

CREATE INDEX idx_banco_etiquetas_etiqueta ON banco_etiquetas (etiqueta_id);
