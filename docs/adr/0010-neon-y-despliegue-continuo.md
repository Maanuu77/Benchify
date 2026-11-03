# ADR-0010: Despliegue gratuito con Firebase Spark, Render y Neon

- **Estado:** Aceptado
- **Fecha:** 2026-11-03
- **Sustituye en parte a:** [ADR-0009](0009-despliegue-cloud-run-firebase-hosting.md) (backend, base de
  datos, fotos y forma de desplegar)

## Contexto

El despliegue tiene que ser **gratuito y sin tarjeta**. ADR-0009 usaba Cloud Run, Cloud SQL y Cloud
Storage: todos exigen el plan Blaze de Firebase, que pide una tarjeta y no tiene un tope de gasto
real. En el plan gratuito (Spark), Firebase mantiene Authentication y Hosting, pero Hosting solo puede
reenviar rutas a Cloud Run o Cloud Functions, que también son de pago.

Alternativas valoradas:

- **Blaze con alertas de presupuesto**: lo ya preparado, con coste cero en la práctica, pero con
  tarjeta y riesgo de cobro.
- **Sustituir Firebase por Supabase** (autenticación, base de datos y fotos): habría que rehacer el
  registro, el inicio de sesión, la verificación y la recuperación de contraseña. Además pausa los
  proyectos gratuitos tras una semana sin uso y hay que reactivarlos a mano.
- **Servir la web desde Netlify** para reenviar `/api` a Render y tener un único dominio: una cuenta
  más; se puede hacer más adelante.

## Decisión

- **Firebase Spark**: Authentication y Hosting, sin cambios en la aplicación.
- **API en Render** (servicio web gratuito, Docker, Fráncfort), definida en `render.yaml`:
  - se despliega sola con cada cambio del backend en `main`;
  - las credenciales de Neon son variables del servicio y la clave del Admin SDK un *Secret File*
    (`GOOGLE_APPLICATION_CREDENTIALS`).
- **Neon** (PostgreSQL 17 con PostGIS, AWS eu-central-1) para los datos **y las fotos**
  (`benchify.almacen.tipo=postgres`, migración V16):
  - las aprobadas se sirven desde la API con caché de un año y las pendientes solo a quien puede verlas;
  - las fotos se guardan a 1600 px para que quepan en los 0,5 GB;
  - se usa el endpoint directo (Flyway necesita bloqueos de sesión) y Hikari no deja conexiones
    ociosas, para que Neon pueda suspenderse.
- **Memoria y arranque**:
  - la imagen usa SerialGC y menos heap para caber en 512 MB;
  - lleva Class Data Sharing: el contexto se arranca una vez al construirla, sin base de datos (perfil
    `cds`), y cada arranque lee las clases ya preparadas. En local, de ~7 s a ~4,5 s.
- **Backend despierto**: Render duerme el servicio tras 15 minutos. Un workflow lo avisa cada 10
  minutos, y un único servicio encendido todo el mes cabe en las 750 horas gratuitas. El aviso no
  toca la base de datos.
- **Frontend en Hosting desde GitHub Actions**, con Workload Identity Federation (sin claves, limitado
  a este repositorio) y canales de vista previa en los PRs.
  - `deploy/configurar-frontend.mjs` escribe la dirección de la API y la configuración web de Firebase.
  - La CSP permite `*.onrender.com`.

## Consecuencias

- Coste cero y sin tarjeta.
- La API está en otro dominio que la web:
  - hace falta CORS, que ya estaba;
  - los enlaces con vista previa (`/s/{id}`) y el sitemap quedan en el dominio de Render;
  - el service worker no guarda las respuestas de la API.
- Si el aviso periódico falla (GitHub retrasa los programados o los desactiva tras 60 días sin
  actividad en el repositorio), la primera visita tras un rato tarda en torno a un minuto.
- El espacio de Neon limita el número de fotos. Volver a Cloud Storage es cambiar
  `benchify.almacen.tipo`.
