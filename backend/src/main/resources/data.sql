-- Etiquetas pre-cargadas según el documento de producto.
-- Se ejecuta tras la creación de tablas por Hibernate.
-- Sintaxis portable entre H2 (modo PostgreSQL) y PostgreSQL real.

INSERT INTO etiquetas (nombre, color, icono)
SELECT * FROM (VALUES
    ('romantico',  '#E74C3C', 'heart'),
    ('relax',      '#3498DB', 'spa'),
    ('vistas',     '#2ECC71', 'landscape'),
    ('sombra',     '#7F8C8D', 'park'),
    ('centrico',   '#F39C12', 'place'),
    ('naturaleza', '#27AE60', 'nature'),
    ('accesible',  '#1ABC9C', 'accessible'),
    ('infantil',   '#E67E22', 'child-care'),
    ('historico',  '#8E44AD', 'account-balance')
) AS v(nombre, color, icono)
WHERE NOT EXISTS (SELECT 1 FROM etiquetas e WHERE e.nombre = v.nombre);
