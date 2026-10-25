# ADR-0005: PostgreSQL + PostGIS con esquema gestionado por Flyway

- **Estado:** Aceptado
- **Fecha:** 2026-10-25

## Contexto

El monolito guardaba latitud y longitud como `double` y filtraba el mapa con `BETWEEN` sobre un
índice B-tree compuesto. No había búsqueda por distancia real, y el esquema lo generaba
Hibernate (`ddl-auto=update`) con un `data.sql` para los datos iniciales, sin historial de cambios
ni forma fiable de reproducirlo en producción. Los tests usaban H2, que no soporta tipos espaciales.

## Decisión

1. **PostgreSQL 17 + PostGIS 3.5** en todos los entornos (docker-compose en desarrollo,
   Testcontainers en tests, Cloud SQL en producción).
2. Cada banco conserva `lat`/`lng` y añade una columna **generada**
   `ubicacion geography(Point, 4326)` con índice GIST. La aplicación solo escribe `lat`/`lng`;
   la base de datos mantiene la geografía coherente.
3. Las consultas espaciales usan `ST_DWithin` (cercanía, en metros) y `&&` con
   `ST_MakeEnvelope` (bounding box del mapa) en consultas nativas del adaptador de persistencia.
   No se añade Hibernate Spatial: el dominio no necesita tipos JTS.
4. **Flyway** es el dueño del esquema: una migración por feature (`V2__usuarios.sql`,
   `V3__bancos.sql`…). Hibernate valida (`ddl-auto=validate`) y `flyway.clean` está desactivado.
5. H2 desaparece. Los tests de integración corren contra PostGIS real.

## Consecuencias

- Búsqueda por radio indexada y precisa (geodésica), con distancia devuelta al cliente.
- El esquema es reproducible y revisable en cada PR.
- Los tests de integración necesitan Docker (o una base externa vía `IT_DATABASE_URL`);
  sin ninguno de los dos se omiten en local y se ejecutan siempre en CI.
- La extensión PostGIS requiere un usuario con permiso para `CREATE EXTENSION`
  (superusuario en Docker; `cloudsqlsuperuser` en Cloud SQL).
