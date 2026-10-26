# ADR-0008: Frontend con Angular 21, AngularFire y Angular Material

- **Estado:** Aceptado
- **Fecha:** 2026-10-26

## Contexto

La interfaz se reescribe como SPA. El plan pedía "Angular, última versión estable" y
AngularFire para la autenticación. A fecha de este ADR la última versión de Angular es la
22.2, pero AngularFire 21.0.0 (la estable) declara compatibilidad solo con Angular `^21.2`;
para Angular 22 únicamente existen builds *canary*.

Alternativas valoradas:

1. **Angular 21.2 + AngularFire 21** (estables y compatibles entre sí).
2. Angular 22.2 + SDK modular de Firebase envuelto a mano, sin AngularFire.
3. Angular 22.2 + AngularFire *canary*.

## Decisión

Opción 1, confirmada con el propietario del producto. Stack:

- **Angular 21.2** sin `zone.js` (zoneless), componentes *standalone*, *signals*
  (`signal`, `computed`, `input`, `model`, `toSignal`) y el nuevo *control flow* (`@if`, `@for`).
- Estructura `core/` (layout, auth, http, config), `shared/` (UI reutilizable) y
  `features/` (una carpeta por pantalla), con *lazy loading* por ruta.
- **AngularFire 21** para Firebase Authentication (email/contraseña y Google), con el emulador
  en desarrollo.
- **Angular Material 21** (Material 3) con tema propio; claro y oscuro según el sistema.
- **Leaflet** + `leaflet.markercluster` para el mapa.
- **Vitest** (runner por defecto de Angular 21) para tests unitarios y **Playwright** para e2e.
- **angular-eslint** con las reglas de accesibilidad de plantillas.

## Consecuencias

- Todas las dependencias son versiones estables y con soporte.
- Pasar a Angular 22 queda en el backlog (#49) a la espera de AngularFire 22 estable;
  `ng update` debería bastar porque no se usan APIs retiradas.
