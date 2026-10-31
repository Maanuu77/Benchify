-- Baja de cuentas (RGPD). El perfil no se borra: se anonimiza para que bancos, reseñas y fotos
-- sigan teniendo autor, y eliminado_en impide que un token antiguo lo vuelva a usar.
ALTER TABLE usuarios ADD COLUMN eliminado_en TIMESTAMPTZ;

-- Claves foráneas a usuarios que no tenían índice: la baja y el perfil las consultan por usuario
CREATE INDEX idx_imagenes_subida_por ON imagenes (subida_por);
CREATE INDEX idx_resena_reportes_usuario ON resena_reportes (usuario_id);
