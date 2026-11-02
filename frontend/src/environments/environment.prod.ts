import { Environment } from './environment.model';

/**
 * Producción (Firebase Hosting). Sustituye los valores de Firebase por los de la app web de tu
 * proyecto (Consola de Firebase → Configuración del proyecto → Tus apps). La API va por el mismo
 * dominio: Firebase Hosting reenvía /api/** a Cloud Run (firebase.json), así que no hay CORS y el
 * service worker puede guardar sus respuestas para usarlas sin conexión.
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
