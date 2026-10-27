# ADR-0002: Monorepo con backend, frontend y documentación

- **Estado:** Aceptado
- **Fecha:** 2026-10-25

## Contexto

El monolito original servía las vistas con Thymeleaf desde el mismo proceso que la API.
Al separar la interfaz en una SPA de Angular hay dos aplicaciones con ciclos de build distintos
(Maven y npm) pero un único producto, un único equipo y cambios que a menudo tocan ambos lados
(un endpoint nuevo y la pantalla que lo consume).

Alternativas:

1. **Dos repositorios** (`benchify-api`, `benchify-web`): aislamiento total, pero los cambios
   transversales requieren dos PRs coordinadas y la documentación queda repartida.
2. **Monorepo con carpetas por aplicación**: un único historial, una única PR por funcionalidad.
3. **Monorepo con herramienta de orquestación** (Nx, Bazel): demasiado peso para dos aplicaciones.

## Decisión

Un único repositorio con `backend/` (Spring Boot), `frontend/` (Angular) y `docs/`.
Cada aplicación conserva su propia herramienta de build. CI ejecuta un job por aplicación y
filtra por ruta para no recompilar lo que no ha cambiado.

## Consecuencias

- Una funcionalidad completa (API + UI + docs) viaja en una sola rama y un solo merge.
- El `.gitignore` y el `.gitattributes` de la raíz cubren ambos ecosistemas.
- Las versiones de backend y frontend se publican juntas (`v1.0.0`).
