import { BancoResumen } from '../../core/api/models';

const CLAVE = 'bf-ultima-zona';

export interface UltimaZona {
  /** Cuándo se cargó, en ISO 8601. */
  fecha: string;
  bancos: BancoResumen[];
}

/**
 * Los bancos de la última zona que se cargó con conexión, para seguir viéndolos sin ella. Se guardan
 * en el navegador y no en el service worker: así solo se conservan datos públicos del mapa y nunca
 * respuestas personales de la API (favoritos, reseñas propias…).
 */
export function guardarUltimaZona(bancos: BancoResumen[], ahora = new Date()): void {
  try {
    localStorage.setItem(CLAVE, JSON.stringify({ fecha: ahora.toISOString(), bancos } satisfies UltimaZona));
  } catch {
    // Sin almacenamiento (modo privado, cuota llena) simplemente no hay copia sin conexión
  }
}

export function leerUltimaZona(): UltimaZona | null {
  try {
    const guardada = JSON.parse(localStorage.getItem(CLAVE) ?? 'null') as UltimaZona | null;
    return guardada && Array.isArray(guardada.bancos) ? guardada : null;
  } catch {
    return null;
  }
}
