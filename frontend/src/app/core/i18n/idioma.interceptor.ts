import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_URL } from '../config/api-url';
import { IDIOMA } from './idioma';

/**
 * Pide a la API las respuestas en el idioma elegido en la aplicación, que puede no ser el del
 * navegador (p. ej. el buscador de direcciones).
 */
export const idiomaInterceptor: HttpInterceptorFn = (request, next) =>
  request.url.startsWith(inject(API_URL))
    ? next(request.clone({ setHeaders: { 'Accept-Language': inject(IDIOMA) } }))
    : next(request);
