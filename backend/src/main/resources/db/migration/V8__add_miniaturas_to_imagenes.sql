-- Miniatura de cada foto, junto al original en el bucket. Las fotos anteriores no tienen: la API
-- devuelve entonces la URL de la foto completa.
ALTER TABLE imagenes
    ADD COLUMN ruta_miniatura VARCHAR(512),
    ADD COLUMN url_miniatura  VARCHAR(1024),
    ADD CONSTRAINT uk_imagenes_ruta_miniatura UNIQUE (ruta_miniatura),
    ADD CONSTRAINT ck_imagenes_miniatura CHECK ((ruta_miniatura IS NULL) = (url_miniatura IS NULL));
