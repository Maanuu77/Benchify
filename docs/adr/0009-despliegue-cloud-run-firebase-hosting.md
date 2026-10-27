# ADR-0009: Backend en Cloud Run y frontend en Firebase Hosting

- **Estado:** Aceptado
- **Fecha:** 2026-10-26

## Contexto

La autenticación y las imágenes ya viven en Firebase (ADR-0006), y la base de datos necesita
PostGIS (ADR-0005). Hay que alojar una API Java con poco tráfico previsible y una SPA estática,
con coste cercano a cero cuando no hay uso y sin gestionar servidores.

Alternativas: una VM o App Engine para el backend; Cloud Storage + CDN o un servidor Nginx
para el frontend.

## Decisión

- **Backend → Cloud Run** a partir de la imagen de `backend/Dockerfile`, escalando de 0 a 4
  instancias. Probes sobre `/actuator/health/readiness` y `/liveness`, *startup CPU boost* para
  reducir el arranque en frío de la JVM.
- **Base de datos → Cloud SQL para PostgreSQL 17** (con PostGIS) mediante el
  `postgres-socket-factory`: TLS autorizado por IAM sin listas de IPs ni proxy aparte.
- **Secretos → Secret Manager** (contraseña de la base de datos), inyectados como variable de
  entorno. Firebase Admin usa la cuenta de servicio de Cloud Run: no hay ficheros de credenciales.
- **Frontend → Firebase Hosting**: CDN global, HTTPS, rewrite de SPA y caché inmutable para los
  ficheros con hash. El mismo `firebase.json` define reglas de Storage y emuladores.
- Despliegue manual con `deploy/cloudrun/deploy.sh` y `firebase deploy`; el despliegue
  continuo desde `main` queda en el backlog (#47).

## Consecuencias

- Coste casi nulo en reposo; la primera petición tras un periodo sin uso paga el arranque en frío.
- Toda la configuración de producción son variables de entorno documentadas en `.env.example`.
- La configuración web de Firebase del frontend se fija en `environment.prod.ts` en el build:
  cambiar de proyecto exige recompilar.
