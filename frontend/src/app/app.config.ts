import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeEnGb from '@angular/common/locales/en-GB';
import localeEs from '@angular/common/locales/es';
import {
  ApplicationConfig, inject, isDevMode, LOCALE_ID, provideAppInitializer, provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { initializeApp, provideFirebaseApp } from '@angular/fire/app';
import { connectAuthEmulator, getAuth, provideAuth } from '@angular/fire/auth';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling, withNavigationErrorHandler } from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';

import { environment } from '../environments/environment';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { revalidarRutaAlPerderAcceso } from './core/auth/guards';
import { IDIOMA, LOCALES } from './core/i18n/idioma';
import { idiomaInterceptor } from './core/i18n/idioma.interceptor';
import { recargarTrasDespliegue } from './core/layout/recargar-tras-despliegue';
import { Actualizaciones } from './core/pwa/actualizaciones';

registerLocaleData(localeEs);
registerLocaleData(localeEnGb);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideAppInitializer(revalidarRutaAlPerderAcceso),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top' }),
      withNavigationErrorHandler(recargarTrasDespliegue),
    ),
    provideHttpClient(withFetch(), withInterceptors([idiomaInterceptor, authInterceptor])),
    provideFirebaseApp(() => initializeApp(environment.firebase)),
    provideAuth(() => {
      const auth = getAuth();
      if (environment.emulators) {
        connectAuthEmulator(auth, environment.emulators.auth, { disableWarnings: true });
      }
      return auth;
    }),
    // Fechas y números en el idioma de la visita; el atributo lang permite a lectores de pantalla y
    // traductores saber en qué idioma está la página
    { provide: LOCALE_ID, useFactory: () => LOCALES[inject(IDIOMA)] },
    provideAppInitializer(() => {
      document.documentElement.lang = inject(IDIOMA);
    }),
    // Solo en producción: con ng serve el service worker serviría versiones antiguas al recargar
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000',
    }),
    provideAppInitializer(() => inject(Actualizaciones).iniciar()),
  ],
};
