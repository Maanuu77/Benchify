-- Usuarios de Benchify. La identidad la gestiona Firebase Authentication:
-- el id es el UID de Firebase y aquí solo se guarda el perfil público y el rol.
CREATE TABLE usuarios (
    id             VARCHAR(128) PRIMARY KEY,
    email          VARCHAR(255),
    nombre         VARCHAR(100) NOT NULL,
    avatar_url     VARCHAR(1024),
    ciudad         VARCHAR(100),
    rol            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    fecha_registro TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ultimo_acceso  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_usuarios_email ON usuarios (lower(email));
CREATE INDEX idx_usuarios_fecha_registro ON usuarios (fecha_registro DESC);
