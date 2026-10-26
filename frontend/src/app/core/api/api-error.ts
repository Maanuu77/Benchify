import { HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, of, startWith } from 'rxjs';

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
  if (!(error instanceof HttpErrorResponse)) {
    return { status: 0, code: 'UNKNOWN', message: 'Se ha producido un error inesperado.', fieldErrors: [] };
  }
  if (error.status === 0) {
    return { status: 0, code: 'NETWORK', message: 'No hay conexión con el servidor. Revisa tu red.', fieldErrors: [] };
  }
  const problem = (typeof error.error === 'object' && error.error !== null ? error.error : {}) as ProblemDetail;
  return {
    status: error.status,
    code: problem.code ?? `HTTP_${error.status}`,
    message: problem.detail ?? mensajePorEstado(error.status),
    fieldErrors: problem.errors ?? [],
  };
}

function mensajePorEstado(status: number): string {
  if (status === 401) return 'Tu sesión ha caducado. Vuelve a iniciar sesión.';
  if (status === 403) return 'No tienes permiso para hacer esto.';
  if (status === 404) return 'No hemos encontrado lo que buscas.';
  if (status >= 500) return 'El servidor no está disponible ahora mismo.';
  return 'No se ha podido completar la operación.';
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
