import { Environment } from './environment.model';

/**
 * Producción (Firebase Hosting + API en Render). No se edita a mano: deploy/configurar-frontend.mjs
 * escribe la dirección de la API y la configuración web de Firebase antes de compilar.
 */
export const environment: Environment = {
  production: true,
  apiUrl: '/api/v1',
  firebase: {
    projectId: 'REPLACE_WITH_FIREBASE_PROJECT_ID',
    apiKey: 'REPLACE_WITH_FIREBASE_WEB_API_KEY',
    authDomain: 'REPLACE_WITH_FIREBASE_PROJECT_ID.firebaseapp.com',
    storageBucket: 'REPLACE_WITH_FIREBASE_PROJECT_ID.appspot.com',
    appId: 'REPLACE_WITH_FIREBASE_APP_ID',
  },
  map: {
    center: [40.4168, -3.7038],
    zoom: 6,
  },
};
