-- Datos de ejemplo SOLO para el perfil dev (application-dev.yml añade classpath:db/seed).
-- Migración repetible e idempotente: Flyway la vuelve a aplicar cuando cambia su contenido.
-- Los UID coinciden con los usuarios que DevUsersSeeder crea en el emulador de Auth.

INSERT INTO usuarios (id, email, nombre, rol, ciudad) VALUES
    ('dev-admin', 'admin@benchify.dev', 'Admin Benchify', 'ADMIN', 'Sevilla'),
    ('dev-user',  'ana@benchify.dev',   'Ana García',     'USER',  'Sevilla')
ON CONFLICT (id) DO NOTHING;

INSERT INTO bancos (id, titulo, descripcion, lat, lng, direccion, ciudad, provincia, estado,
                    con_sombra, accesible, con_respaldo, material, creado_por, fecha_subida, fecha_verificacion)
VALUES
    ('00000000-0000-4000-8000-000000000001', 'Banco frente a la Torre del Oro',
     'Vistas al Guadalquivir y a Triana. Muy concurrido al atardecer.',
     37.38250, -5.99600, 'Paseo de Cristóbal Colón', 'Sevilla', 'Sevilla', 'PUBLICADO',
     FALSE, TRUE, TRUE, 'MADERA', 'dev-admin', now() - interval '30 days', now() - interval '29 days'),
    ('00000000-0000-4000-8000-000000000002', 'Calle Betis, junto al puente',
     'El clásico banco de Triana mirando a Sevilla.',
     37.38440, -6.00230, 'Calle Betis', 'Sevilla', 'Sevilla', 'PUBLICADO',
     FALSE, TRUE, FALSE, 'METAL', 'dev-user', now() - interval '25 days', now() - interval '24 days'),
    ('00000000-0000-4000-8000-000000000003', 'Glorieta de Bécquer',
     'Rincón tranquilo y con sombra en el Parque de María Luisa.',
     37.37630, -5.98920, 'Parque de María Luisa', 'Sevilla', 'Sevilla', 'PUBLICADO',
     TRUE, FALSE, TRUE, 'PIEDRA', 'dev-admin', now() - interval '20 days', now() - interval '19 days'),
    ('00000000-0000-4000-8000-000000000004', 'Plaza de España, bancos de azulejos',
     'Los bancos de cerámica de cada provincia.',
     37.37720, -5.98680, 'Plaza de España', 'Sevilla', 'Sevilla', 'PUBLICADO',
     FALSE, TRUE, TRUE, 'PIEDRA', 'dev-admin', now() - interval '18 days', now() - interval '17 days'),
    ('00000000-0000-4000-8000-000000000005', 'Alameda de Hércules',
     'Bajo los árboles, con terrazas alrededor.',
     37.39960, -5.99390, 'Alameda de Hércules', 'Sevilla', 'Sevilla', 'PUBLICADO',
     TRUE, TRUE, TRUE, 'MADERA', 'dev-user', now() - interval '12 days', now() - interval '11 days'),
    ('00000000-0000-4000-8000-000000000006', 'Jardines de Murillo',
     'Junto a la muralla del Alcázar, fresco en verano.',
     37.38260, -5.98840, 'Paseo de Catalina de Ribera', 'Sevilla', 'Sevilla', 'PUBLICADO',
     TRUE, FALSE, TRUE, 'MADERA', 'dev-admin', now() - interval '9 days', now() - interval '8 days'),
    ('00000000-0000-4000-8000-000000000007', 'Mirador de Las Setas',
     'Sobre la plaza de la Encarnación, ideal para descansar tras subir.',
     37.39320, -5.99170, 'Plaza de la Encarnación', 'Sevilla', 'Sevilla', 'PUBLICADO',
     FALSE, TRUE, FALSE, 'HORMIGON', 'dev-user', now() - interval '6 days', now() - interval '5 days'),
    ('00000000-0000-4000-8000-000000000008', 'Banco escondido en el Barrio de Santa Cruz',
     'Pendiente de revisar: lo ha propuesto Ana.',
     37.38500, -5.98980, 'Callejón del Agua', 'Sevilla', 'Sevilla', 'PENDIENTE',
     TRUE, FALSE, TRUE, 'PIEDRA', 'dev-user', now() - interval '1 day', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO banco_etiquetas (banco_id, etiqueta_id)
SELECT b.id::uuid, e.id
FROM (VALUES
    ('00000000-0000-4000-8000-000000000001', 'vistas'),
    ('00000000-0000-4000-8000-000000000001', 'romantico'),
    ('00000000-0000-4000-8000-000000000002', 'vistas'),
    ('00000000-0000-4000-8000-000000000002', 'centrico'),
    ('00000000-0000-4000-8000-000000000003', 'sombra'),
    ('00000000-0000-4000-8000-000000000003', 'naturaleza'),
    ('00000000-0000-4000-8000-000000000004', 'historico'),
    ('00000000-0000-4000-8000-000000000005', 'relax'),
    ('00000000-0000-4000-8000-000000000005', 'infantil'),
    ('00000000-0000-4000-8000-000000000006', 'sombra'),
    ('00000000-0000-4000-8000-000000000007', 'vistas'),
    ('00000000-0000-4000-8000-000000000008', 'romantico')
) AS b(id, etiqueta)
JOIN etiquetas e ON e.nombre = b.etiqueta
ON CONFLICT DO NOTHING;
