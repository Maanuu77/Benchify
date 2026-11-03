# ADR-0010: Neon como base de datos y despliegue continuo sin claves

- **Estado:** Aceptado
- **Fecha:** 2026-11-03
- **Sustituye en parte a:** [ADR-0009](0009-despliegue-cloud-run-firebase-hosting.md) (base de datos,
  región y forma de desplegar)

## Contexto

ADR-0009 eligió Cloud SQL, que no tiene capa gratuita: la instancia más pequeña con PostGIS cuesta
dinero aunque nadie use la aplicación, y el resto del despliegue (Cloud Run, Hosting) sí escala a
cero. Además el despliegue era manual y la API vivía en una URL de Cloud Run distinta de la web.

## Decisión

- **Base de datos → Neon** (PostgreSQL 17 con PostGIS, capa gratuita), en *AWS eu-central-1*
  (Fráncfort):
  - Se suspende sin uso, igual que Cloud Run.
  - La conexión (JDBC con `sslmode=require`, usuario y contraseña) llega desde Secret Manager.
  - Se usa el endpoint directo y no el pooler, porque Flyway necesita bloqueos de sesión.
  - Hikari no mantiene conexiones ociosas que impidan la suspensión.
  - Desaparece el conector de Cloud SQL.
- **Cloud Run → `europe-west1`**, a pocos milisegundos de Neon y entre las regiones que Hosting
  admite para reescribir rutas a Cloud Run.
- **API por el dominio de Hosting** (`/api/**` → Cloud Run): sin CORS ni URL de Cloud Run en el
  build, y la PWA puede guardar respuestas.
- **Arranque en frío** reducido con Class Data Sharing: la imagen ejecuta una vez el contexto (perfil
  `cds`, sin base de datos) y guarda las clases cargadas. En local, de ~7 s a ~4,5 s.
- **Despliegue continuo con GitHub Actions** y Workload Identity Federation:
  - No hay claves de cuentas de servicio; el proveedor solo acepta tokens de este repositorio.
  - Al llegar a `main` se despliegan backend y frontend.
  - Los PRs del frontend tienen canal de vista previa.
  - `deploy/preparar-proyecto.sh` crea todo lo necesario una vez.

## Consecuencias

- Coste cero en reposo con el uso previsto. La primera petición tras un rato sin tráfico paga el
  arranque de Cloud Run y la reactivación de Neon.
- Neon y Google Cloud son dos proveedores; las credenciales de Neon solo están en Secret Manager.
- La configuración web de Firebase ya no se escribe a mano: el despliegue la genera a partir de las
  variables del repositorio.
