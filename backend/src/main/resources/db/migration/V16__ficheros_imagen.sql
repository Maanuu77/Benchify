-- Ficheros de las fotos guardados en la propia base de datos (benchify.almacen.tipo=postgres): el
-- despliegue gratuito no tiene Cloud Storage. Las rutas son las mismas que en el bucket
-- (bancos/... públicas, pendientes/... privadas).
CREATE TABLE ficheros_imagen (
    ruta         VARCHAR(512) PRIMARY KEY,
    contenido    BYTEA        NOT NULL,
    content_type VARCHAR(50)  NOT NULL,
    fecha        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
