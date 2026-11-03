import { HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, of, startWith } from 'rxjs';
import { idiomaActual, textosActuales } from '../i18n/idioma';
import { ERRORES, MENSAJES_EN } from './api-error.i18n';

/** Error de la API normalizado a partir del ProblemDetail (RFC 9457) del backend. */
export interface ApiError {
  status: number;
  /** Código estable del backend (p. ej. RESENA_DUPLICADA) o uno local. */
  code: string;
  message: string;
  fieldErrors: { field: string; message: string }[];
}

interface ProblemDetail {
  status?: number;
  detail?: string;
  code?: string;
  errors?: { field: string; message: string }[];
}

export function toApiError(error: unknown): ApiError {
  const t = textosActuales(ERRORES);
  if (!(error instanceof HttpErrorResponse)) {
    return { status: 0, code: 'UNKNOWN', message: t.inesperado, fieldErrors: [] };
  }
  if (error.status === 0) {
    return { status: 0, code: 'NETWORK', message: t.sinConexion, fieldErrors: [] };
  }
  const problem = (typeof error.error === 'object' && error.error !== null ? error.error : {}) as ProblemDetail;
  const code = problem.code ?? `HTTP_${error.status}`;
  return {
    status: error.status,
    code,
    message: mensaje(code, problem.detail, error.status),
    fieldErrors: problem.errors ?? [],
  };
}

/** El mensaje del backend está en español: en inglés se usa su traducción o el genérico de su estado. */
function mensaje(code: string, detalle: string | undefined, status: number): string {
  if (idiomaActual() === 'es') {
    return detalle ?? mensajePorEstado(status);
  }
  return MENSAJES_EN[code] ?? mensajePorEstado(status);
}

function mensajePorEstado(status: number): string {
  const t = textosActuales(ERRORES);
  if (status === 401) return t.sesionCaducada;
  if (status === 403) return t.sinPermiso;
  if (status === 404) return t.noEncontrado;
  if (status >= 500) return t.servidor;
  return t.generico;
}

/** Estado de una carga asíncrona para pintar spinners, errores y datos desde una signal. */
export type Loadable<T> =
  | { status: 'loading' }
  | { status: 'error'; error: ApiError }
  | { status: 'success'; value: T };

export function loadable<T>(source: Observable<T>): Observable<Loadable<T>> {
  return source.pipe(
    map((value): Loadable<T> => ({ status: 'success', value })),
    startWith<Loadable<T>>({ status: 'loading' }),
    catchError((error) => of<Loadable<T>>({ status: 'error', error: toApiError(error) })),
  );
}
