import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { API_URL } from '../config/api-url';
import { AuthService } from './auth.service';

/**
 * Añade `Authorization: Bearer <ID token>` a las peticiones a la API de Benchify. Nunca lo envía
 * a otros dominios (teselas del mapa, Storage…).
 *
 * Si la API responde 401 o 403 con sesión iniciada, pide un token nuevo y reintenta una sola vez:
 * el token puede haber caducado o el rol haber cambiado en el backend (lo manda la base de datos).
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const apiUrl = inject(API_URL);
  if (!request.url.startsWith(apiUrl)) {
    return next(request);
  }
  const auth = inject(AuthService);
  const send = (forceRefresh: boolean) =>
    from(auth.idToken(forceRefresh)).pipe(switchMap((token) => next(withToken(request, token))));

  return send(false).pipe(
    catchError((error: unknown) =>
      error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403) && auth.isAuthenticated()
        ? send(true)
        : throwError(() => error),
    ),
  );
};

function withToken<T>(request: HttpRequest<T>, token: string | null): HttpRequest<T> {
  return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
}
