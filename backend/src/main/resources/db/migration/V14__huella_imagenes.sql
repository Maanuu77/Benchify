-- Huella perceptual (dHash de 64 bits) de cada foto para avisar en moderación de posibles
-- duplicados. Las fotos subidas antes de esta migración no la tienen.
ALTER TABLE imagenes ADD COLUMN huella BIGINT;

COMMENT ON COLUMN imagenes.huella IS
    'dHash de 64 bits: dos fotos casi iguales difieren en pocos bits (distancia de Hamming)';
