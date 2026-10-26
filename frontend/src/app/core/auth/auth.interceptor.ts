import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { API_URL } from '../config/api-url';
import { AuthService } from './auth.service';

/**
 * Añade `Authorization: Bearer <ID token>` a las peticiones a la API de Benchify. Nunca lo envía
 * a otros dominios (teselas del mapa, Storage…).
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const apiUrl = inject(API_URL);
  if (!request.url.startsWith(apiUrl)) {
    return next(request);
  }
  const auth = inject(AuthService);
  return from(auth.idToken()).pipe(
    switchMap((token) =>
      next(token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request),
    ),
  );
};
