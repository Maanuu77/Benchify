import { InjectionToken } from '@angular/core';
import { environment } from '../../../environments/environment';

/** Base de la API REST; inyectable para poder cambiarla en los tests. */
export const API_URL = new InjectionToken<string>('API_URL', {
  providedIn: 'root',
  factory: () => environment.apiUrl,
});
