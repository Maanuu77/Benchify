import { InjectionToken } from '@angular/core';
import { initializeApp } from 'firebase/app';
import { Auth, connectAuthEmulator, getAuth } from 'firebase/auth';
import { environment } from '../../../environments/environment';

/**
 * Auth de Firebase con el SDK modular, sin AngularFire: la aplicación es zoneless y solo usa
 * autenticación, así que el envoltorio no aportaba nada y ataba la versión de Angular a la suya.
 * Se crea al primer uso (al arrancar AuthService); los tests lo sustituyen por un doble.
 */
export const FIREBASE_AUTH = new InjectionToken<Auth>('FIREBASE_AUTH', {
  providedIn: 'root',
  factory: () => {
    const auth = getAuth(initializeApp(environment.firebase));
    if (environment.emulators) {
      connectAuthEmulator(auth, environment.emulators.auth, { disableWarnings: true });
    }
    return auth;
  },
});
