# ADR-0001: Registrar las decisiones de arquitectura

- **Estado:** Aceptado
- **Fecha:** 2026-10-25

## Contexto

La migración de Benchify cambia casi todas las piezas del sistema (framework de vistas,
autenticación, base de datos, despliegue). Sin un registro, el motivo de cada cambio se pierde
en el historial de commits y es difícil de revisar o revertir.

## Decisión

Usamos *Architecture Decision Records* en formato ligero (Michael Nygard) en `docs/adr/`,
numerados de forma correlativa (`NNNN-titulo.md`) y escritos a partir de [`template.md`](template.md).
Un ADR aceptado no se edita: si la decisión cambia se escribe uno nuevo que lo sustituye.

## Consecuencias

- Cada decisión relevante tiene contexto, alternativas y consecuencias por escrito.
- Añadir un ADR forma parte de la rama que introduce el cambio.
