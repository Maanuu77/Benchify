# Benchify — Roadmap de modernización

Benchify es un mapa colaborativo de bancos públicos: los usuarios descubren bancos cercanos,
los valoran, los guardan en favoritos, hacen check-in y suben fotos; el equipo de moderación
revisa el contenido antes de publicarlo.

Este documento recoge el backlog de la migración del monolito original
(Spring Boot + Thymeleaf + HTMX + JWT propio) a una arquitectura moderna:
API REST hexagonal, frontend Angular, Firebase y PostGIS.

## Convenciones

- Cada issue tiene un número `#n` que se referencia en los commits (Conventional Commits, en inglés).
- Git Flow: `main` (estable) ← `release/*` ← `develop` ← ramas de trabajo
  (`feature/backend-*`, `feature/frontend-*`, `fix/*`, `refactor/*`, `chore/*`, `ci/*`, `docs/*`).
- Las ramas se integran en `develop` con `git merge --no-ff` y un mensaje tipo PR
  (qué, por qué, cómo probarlo). Antes de cada merge el proyecto compila y los tests pasan.
- Las decisiones técnicas relevantes se registran como ADR en [`docs/adr/`](adr/).

Estado: ✅ hecho · 🚧 en curso · ⬜ pendiente

---

## Sprint 0 — Limpieza y monorepo

| #  | Issue | Rama | Estado |
|----|-------|------|--------|
| #1 | Crear `develop` y el roadmap con el backlog por sprints | `docs/roadmap` | ✅ |
| #2 | Reestructurar en monorepo: `/backend`, `/frontend`, `/docs` | `chore/monorepo-structure` | ✅ |
| #3 | Eliminar `target/`, `node_modules/` y ficheros de IDE; `.gitignore` raíz | `chore/monorepo-structure` | ✅ |
| #4 | Proceso de ADRs y decisiones iniciales (monorepo, versión de Spring Boot, hexagonal) | `docs/adr-initial` | ✅ |

## Sprint 1 — Backend: API REST hexagonal

| #   | Issue | Rama | Estado |
|-----|-------|------|--------|
| #5  | Actualizar build: Spring Boot 4.1, Java 21, MapStruct, springdoc, Flyway, JaCoCo, ArchUnit, Testcontainers | `chore/backend-build-upgrade` | ✅ |
| #6  | Eliminar Thymeleaf, plantillas, estáticos y controladores web | `refactor/backend-remove-thymeleaf` | ✅ |
| #7  | Perfiles `dev` / `test` / `prod` y CORS configurable por perfil | `feature/backend-profiles-cors` | ✅ |
| #8  | Esquema con Flyway + PostGIS (sustituye `data.sql` y `ddl-auto`) | `feature/backend-flyway-postgis` | ✅ |
| #9  | Núcleo compartido: errores RFC 9457 (`ProblemDetail`), paginación, validación | `feature/backend-shared-kernel` | ✅ |
| #48 | Retirar la implementación por capas heredada antes de reescribir por features | `refactor/backend-remove-legacy` | ✅ |
| #10 | Autenticación con Firebase: filtro que verifica el ID token, roles por custom claims, eliminar JWT propio | `feature/backend-firebase-auth` | ✅ |
| #11 | Feature `usuario` (hexagonal): alta automática, perfil, cambio de rol, estadísticas | `feature/backend-usuario`, `feature/backend-usuario-estadisticas` | ✅ |
| #12 | Feature `banco` (hexagonal): CRUD, etiquetas, bounding box | `feature/backend-banco` | ✅ |
| #13 | Búsqueda de bancos por cercanía con PostGIS (`ST_DWithin`) | `feature/backend-banco` | ✅ |
| #14 | Imágenes de bancos en Firebase Cloud Storage | `feature/backend-banco-imagenes` | ✅ |
| #15 | Feature `resena` (hexagonal): reseñas, reportes, media de puntuación | `feature/backend-resena` | ✅ |
| #16 | Feature `favorito` (hexagonal) | `feature/backend-favorito` | ✅ |
| #17 | Feature `checkin` (hexagonal) con validación de proximidad | `feature/backend-checkin` | ✅ |
| #18 | Feature `moderacion` (hexagonal): bancos, reseñas, imágenes, estadísticas | `feature/backend-moderacion` | ✅ |
| #19 | Documentación OpenAPI con springdoc + Swagger UI | `feature/backend-openapi` | ✅ |
| #20 | Reglas ArchUnit para la arquitectura hexagonal | `feature/backend-archunit` | ✅ |
| #21 | Tests de integración con Testcontainers (PostGIS) | `feature/backend-integration-tests` | ✅ |
| #22 | Cobertura JaCoCo ≥ 70 % como umbral del build | `feature/backend-integration-tests` | ✅ |

## Sprint 2 — Frontend Angular

| #   | Issue | Rama | Estado |
|-----|-------|------|--------|
| #23 | Workspace Angular: standalone, signals, control flow, estructura core/shared/features, Angular Material | `feature/frontend-scaffold` | ⬜ |
| #24 | Firebase Auth en cliente (AngularFire): email/contraseña y Google | `feature/frontend-auth` | ⬜ |
| #25 | Interceptor HTTP con el ID token y guards por rol | `feature/frontend-auth` | ⬜ |
| #26 | Cliente de la API: modelos y servicios por feature | `feature/frontend-api-client` | ⬜ |
| #27 | Mapa con Leaflet: marcadores, clustering, búsqueda por cercanía | `feature/frontend-map` | ⬜ |
| #28 | Detalle de banco: reseñas, favorito, check-in, galería | `feature/frontend-bench-detail` | ⬜ |
| #29 | Alta de banco con selección en mapa y subida de imágenes | `feature/frontend-bench-create` | ⬜ |
| #30 | Perfil de usuario: favoritos, check-ins y bancos subidos | `feature/frontend-profile` | ⬜ |
| #31 | Panel de administración y moderación | `feature/frontend-admin` | ⬜ |
| #32 | Estados de carga/error, diseño responsive y accesibilidad | (transversal) | ⬜ |
| #33 | Tests unitarios de servicios y componentes (Vitest) | (transversal) | ⬜ |
| #34 | Tests e2e básicos con Playwright | `feature/frontend-e2e` | ⬜ |

## Sprint 3 — DevOps y documentación

| #   | Issue | Rama | Estado |
|-----|-------|------|--------|
| #35 | Dockerfile multi-stage del backend | `chore/backend-dockerfile` | ⬜ |
| #36 | `docker-compose`: PostgreSQL + PostGIS y Firebase Emulator Suite | `chore/docker-compose` | ⬜ |
| #37 | GitHub Actions: job backend (build, test, cobertura) | `ci/github-actions` | ⬜ |
| #38 | GitHub Actions: job frontend (lint, test, build) | `ci/github-actions` | ⬜ |
| #39 | Despliegue: backend en Cloud Run | `chore/deploy-config` | ⬜ |
| #40 | Despliegue: frontend en Firebase Hosting (`firebase.json`, `.firebaserc`, environments) | `chore/deploy-config` | ⬜ |
| #41 | README profesional con diagrama Mermaid, setup y badges; `.env.example` | `docs/readme` | ⬜ |
| #42 | Release 1.0.0: `CHANGELOG.md`, merge a `main` y tag `v1.0.0` | `release/1.0.0` | ⬜ |

## Después de la 1.0 (backlog)

| #   | Issue |
|-----|-------|
| #43 | Notificaciones al autor cuando se aprueba o rechaza su banco |
| #44 | Detección de imágenes duplicadas (hash perceptual) |
| #45 | Internacionalización del frontend (es / en) |
| #46 | Caché de lecturas del mapa (HTTP caching / ETag) |
| #47 | Despliegue continuo desde `main` a Cloud Run y Firebase Hosting |
