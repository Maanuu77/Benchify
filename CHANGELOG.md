# Changelog

Todos los cambios relevantes de Benchify. Formato basado en
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); el proyecto sigue
[versionado semántico](https://semver.org/lang/es/). Los números `#n` son issues del
[roadmap](docs/ROADMAP.md).

## [Sin publicar]

Desde la 1.0.1 los números `#n` son issues de GitHub
([tablero](https://github.com/users/Maanuu77/projects/1)); los de versiones anteriores son del roadmap antiguo.

### Seguridad

- Las fotos pendientes de moderación ya no son públicas: se guardan en `pendientes/` y solo las
  ven su autor y los administradores a través de la API; al aprobarlas pasan a la ruta pública.
  `storage.rules` deja de permitir listar carpetas y las fotos públicas ya no se cachean como
  inmutables (#14).
- El rol de la base de datos es el que autoriza. Al cambiarlo se revocan las sesiones del usuario
  y un token antiguo ya no puede volver a escribir el rol anterior (#15).
- Las fotos se recodifican a JPEG sin metadatos EXIF/GPS, con un máximo de 2048 px, una miniatura
  de 480 px y un límite de 40 megapíxeles (#27).
- Límite de frecuencia en las escrituras: 30 por minuto y 10 subidas de fotos cada 10 minutos por
  usuario, con 429 `DEMASIADAS_PETICIONES` y `Retry-After`. Un usuario puede tener como máximo
  5 bancos pendientes (#26).

- Firebase Hosting envía `Content-Security-Policy`, `X-Frame-Options: DENY` y
  `Cross-Origin-Opener-Policy`, y las rutas de la SPA ya no se cachean (#32).

### Corregido

- Frontend: las primeras peticiones tras recargar ya llevan el token, los guards no se quedan
  esperando si falla la restauración de la sesión, y ante un 401/403 se pide un token nuevo y se
  reintenta una vez; perder la sesión o el rol en otra pestaña saca de la página protegida (#15, #17, #18).
- Frontend: la ficha y el perfil se refrescan sin desmontarse, se descartan las respuestas de otro
  banco o sesión y el formulario de reseña sabe si ya has reseñado el banco (#20, #30, #31, #35).
- Mapa: recorrer la lista con el teclado ya no la vacía, "Cerca de mí" respeta los filtros, el área
  pedida siempre es válida, se avisa del límite de 500 bancos y se puede reintentar (#21, #28, #29).
- Moderación: "Cargar más" ya no se salta elementos tras moderar (#19).
- Formulario de banco: aviso de cambios sin guardar, comprobación del autor, mensajes de
  coordenadas y edición con `If-Match` (#37, #38). Estrellas accesibles (#36).
- Las subidas a Storage ya no ocupan una conexión de la base de datos, los ficheros se borran solo
  después del commit y un fallo al guardar no deja ficheros huérfanos (#16).
- Condiciones de carrera en el límite de 10 fotos por banco y en el intervalo de 12 h entre
  check-ins (#23), y en los reportes de reseñas, que ya no se pierden ante ediciones simultáneas (#24).
- El primer inicio de sesión con peticiones en paralelo ya no devuelve 500; una violación de
  restricción en la base de datos responde 409 `CONFLICTO_DATOS` (#22).
- Las reseñas y estadísticas de un perfil y el total de check-ins ya no muestran datos de bancos
  no publicados (#25).
- Al borrar un banco se borran también sus fotos del bucket (#33).
- `ultimo_acceso` se actualiza cada 15 minutos de uso (#34).
- Las cabeceras `Location` de reseñas e imágenes apuntan a recursos que existen (#38).

### Añadido

- `GET /api/v1/bancos/{id}/resenas/mia`, `GET /api/v1/resenas/{id}` y `GET /api/v1/imagenes/{id}`
  (con `/archivo` y `/miniatura` para las fotos pendientes) (#14, #31, #38).
- `ETag` en la ficha de un banco y `If-Match` opcional al editarlo (412 `BANCO_VERSION_OBSOLETA`) (#38).
- Las imágenes exponen `thumbUrl` (#27). La galería y la moderación usan miniaturas, las fotos
  pendientes se descargan con el token y las fotos se reducen a 1600 px en WebP en el navegador
  antes de subirlas (#27).

### Cambiado

- Límite de subida de fotos unificado en 10 MB (#27) y máximo de 10000 en el número de página (#41).
- Migraciones V8 (miniaturas de las imágenes) y V10 (versión de las reseñas para el bloqueo optimista).

## [1.0.1] - 2026-10-27

### Corregido

- El wrapper de Maven (`backend/mvnw`) no tenía permiso de ejecución en el repositorio y el job
  de backend de GitHub Actions fallaba con el código 126 (#37).

## [1.0.0] - 2026-10-26

Primera versión de la reescritura: el monolito Spring Boot + Thymeleaf + HTMX pasa a un monorepo
con una API REST hexagonal y una SPA Angular, con Firebase y PostGIS.

### Añadido

- **Monorepo** `backend/`, `frontend/` y `docs/`, con roadmap y ADRs 0001–0009 (#1–#4).
- **API REST `/api/v1`** en Spring Boot 4.1 y Java 21 con arquitectura hexagonal por feature:
  `banco`, `resena`, `usuario`, `favorito`, `checkin` y `moderacion` (#5–#18).
  - Bancos: propuesta, edición, borrado lógico, etiquetas, búsqueda por bounding box y por
    cercanía con PostGIS (`ST_DWithin`) y filtros (#12, #13).
  - Fotos en Firebase Cloud Storage con validación del contenido real (JPEG/PNG/WebP, 5 MB) (#14).
  - Reseñas con media desnormalizada consistente ante escrituras concurrentes y reportes únicos
    por usuario (#15).
  - Favoritos idempotentes (#16) y check-ins validados por proximidad y tiempo (#17).
  - Panel de moderación: bancos, reseñas reportadas, fotos y estadísticas (#18).
  - Perfil propio y público con estadísticas de actividad (#11).
- **Autenticación con Firebase**: filtro que verifica el ID token, roles USER/ADMIN como custom
  claims, alta automática del perfil local y gestión de roles (#10, #11).
- **Errores RFC 9457** (`ProblemDetail`) con `code` estable, validación con Bean Validation y
  paginación común (#9).
- **Esquema con Flyway** (V1–V7) y datos de ejemplo de Sevilla en el perfil `dev` (#8).
- **Perfiles** `dev`, `test` y `prod` con CORS configurable (#7).
- **OpenAPI 3 / Swagger UI** con el esquema de seguridad de Firebase (#19).
- **Tests de backend**: unitarios, slices web, integración con Testcontainers (PostGIS),
  flujos completos de la API, 11 reglas ArchUnit y umbral JaCoCo (#20–#22).
- **Frontend Angular 21** (standalone, zoneless, signals, control flow) con Angular Material 3 (#23):
  - Login y registro con email/contraseña y Google mediante AngularFire; interceptor del ID token
    y guards por rol (#24, #25).
  - Mapa Leaflet con clustering, filtros y "cerca de mí" (#27).
  - Ficha de banco con reseñas, fotos, favorito y check-in (#28).
  - Alta y edición con selector de ubicación y subida de fotos (#29).
  - Perfil (#30) y panel de administración (#31).
  - Estados de carga/error/vacío, diseño responsive, modo oscuro y accesibilidad (#32).
  - Tests con Vitest (#33) y e2e con Playwright en escritorio, móvil y contra el stack real (#34).
- **DevOps**: Dockerfile multi-stage con usuario sin privilegios (#35), docker compose con
  PostGIS y Firebase Emulator Suite (#36), GitHub Actions para backend y frontend y Dependabot
  (#37, #38), despliegue en Cloud Run con Cloud SQL y Secret Manager (#39) y en Firebase Hosting
  (#40).
- README con arquitectura, guía de puesta en marcha, capturas y `.env.example` (#41).

### Cambiado

- Spring Boot 4.0.6 → 4.1.2 y artefacto `benchify-backend` (#5).
- Usuarios identificados por el UID de Firebase en lugar de un id numérico (#11).
- La ubicación de los bancos es una columna `geography` generada a partir de lat/lng (#12).

### Eliminado

- Vistas Thymeleaf, HTMX, plantillas y recursos estáticos (#6).
- JWT propio, almacenamiento de contraseñas y el secreto en `application.properties` (#10).
- H2, `data.sql` y `ddl-auto=update` (#8).
- Implementación por capas anterior (#48).

### Seguridad

- Ningún secreto en el repositorio: credenciales por Application Default Credentials y Secret Manager.
- Reglas de Storage: lectura pública de fotos de bancos y ninguna escritura desde el cliente.
- Respuestas 401/403 sin filtrar detalles internos; los errores inesperados no exponen mensajes.

[1.0.1]: https://github.com/Maanuu77/Benchify/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/Maanuu77/Benchify/releases/tag/v1.0.0
