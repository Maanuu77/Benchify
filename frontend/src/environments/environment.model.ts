export interface Environment {
  production: boolean;
  /** Base de la API REST, sin barra final. */
  apiUrl: string;
  /**
   * Configuración web de Firebase. No es un secreto: identifica el proyecto y la protegen
   * las reglas de seguridad y las restricciones de la clave en Google Cloud.
   */
  firebase: {
    projectId: string;
    apiKey: string;
    authDomain: string;
    storageBucket: string;
    appId: string;
  };
  /** URLs de Firebase Emulator Suite; ausentes en producción. */
  emulators?: {
    auth: string;
  };
  map: {
    center: [number, number];
    zoom: number;
  };
}
