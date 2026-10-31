import { NavigationError } from '@angular/router';

const CLAVE = 'bf-recarga-tras-despliegue';

/**
 * Tras un despliegue, una pestaña abierta conserva el index.html antiguo y sus chunks ya no existen:
 * al navegar a una ruta lazy el import() falla. Se recarga la página una vez para traer la versión
 * nueva; si vuelve a fallar con la misma URL, se deja el error (no es un despliegue).
 */
export function recargarTrasDespliegue(event: NavigationError): void {
  if (!esErrorDeCarga(event.error)) {
    return;
  }
  try {
    if (sessionStorage.getItem(CLAVE) === event.url) {
      return;
    }
    sessionStorage.setItem(CLAVE, event.url);
  } catch {
    // Sin sessionStorage se recarga igualmente: lo peor es una recarga de más
  }
  window.location.assign(event.url);
}

export function esErrorDeCarga(error: unknown): boolean {
  const mensaje = error instanceof Error ? `${error.name} ${error.message}` : String(error);
  return /ChunkLoadError|Failed to fetch dynamically imported module|Importing a module script failed|error loading dynamically imported module/i
    .test(mensaje);
}
