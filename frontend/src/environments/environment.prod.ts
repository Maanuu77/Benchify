import { Environment } from './environment.model';

/**
 * Producción (Firebase Hosting). Sustituye los valores de Firebase por los de la app web de tu
 * proyecto (Consola de Firebase → Configuración del proyecto → Tus apps) y la URL de Cloud Run.
 */
export const environment: Environment = {
  production: true,
  apiUrl: 'https://benchify-api-REPLACE_WITH_CLOUD_RUN_HASH-ew.a.run.app/api/v1',
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
