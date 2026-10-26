# Benchify · Frontend

SPA en **Angular 21** (standalone, zoneless, signals) con Angular Material, AngularFire y Leaflet.
Consume la API REST del [backend](../backend) y se despliega en Firebase Hosting.

## Estructura

```
src/app/
├── core/       Servicios de toda la app: auth (Firebase, guards, interceptor), api, geo, layout, ui
├── shared/     Componentes reutilizables: mapa (Leaflet), estados de carga/error/vacío, valoración…
└── features/   Una carpeta por pantalla, cargadas bajo demanda: mapa, bancos, perfil, auth, admin
```

## Scripts

| Comando | Qué hace |
|---------|----------|
| `npm start` | Servidor de desarrollo en http://localhost:4200 |
| `npm test -- --watch=false` | Tests unitarios (Vitest) |
| `npm run lint` | ESLint con reglas de Angular y de accesibilidad |
| `npm run build` | Build de producción en `dist/` |
| `npm run e2e` | E2E con Playwright (escritorio y móvil) con la API simulada |
| `npm run e2e:stack` | E2E contra el backend y Firebase Emulator Suite reales |

En local puedes usar un navegador instalado en lugar de descargar Chromium:
`PW_CHANNEL=msedge npm run e2e` (o `chrome`).

## Configuración

- `src/environments/environment.ts`: desarrollo, apunta a `http://localhost:8080/api/v1` y al
  emulador de Auth del proyecto `demo-benchify`.
- `src/environments/environment.prod.ts`: producción; rellena la configuración web de Firebase
  y la URL de Cloud Run antes de desplegar.

Usuarios de prueba en desarrollo (los crea el backend al arrancar con el perfil `dev`):
`admin@benchify.dev` (administrador) y `ana@benchify.dev`, con la contraseña definida en
`SEED_USERS_PASSWORD` (por defecto, la de `backend/src/main/resources/application-dev.yml`).
