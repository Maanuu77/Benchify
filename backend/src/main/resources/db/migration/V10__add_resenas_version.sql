-- Bloqueo optimista de las reseñas: una edición basada en una lectura antigua falla (409) en lugar
-- de pisar otra escritura simultánea. Los contadores de reportes no dependen de esta columna: se
-- actualizan con UPDATE atómicos para que un reporte nunca se pierda ni haga fallar una edición.
ALTER TABLE resenas ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
