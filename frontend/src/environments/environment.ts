import { Environment } from './environment.model';

/**
 * Desarrollo local: backend en localhost:8080 y Firebase Emulator Suite con el proyecto
 * de demostración `demo-benchify` (no necesita credenciales reales).
 */
export const environment: Environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  firebase: {
    projectId: 'demo-benchify',
    apiKey: 'demo-api-key',
    authDomain: 'demo-benchify.firebaseapp.com',
    storageBucket: 'demo-benchify.appspot.com',
    appId: 'demo-app-id',
  },
  emulators: {
    auth: 'http://127.0.0.1:9099',
  },
  map: {
    // Sevilla
    center: [37.3886, -5.9823],
    zoom: 14,
  },
};
