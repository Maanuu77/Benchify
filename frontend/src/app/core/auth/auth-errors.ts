import { textosActuales } from '../i18n/idioma';
import { ERRORES_AUTH } from './auth-errors.i18n';

/** Traduce los códigos de error de Firebase Auth a mensajes para el usuario. */
export function mensajeDeErrorAuth(error: unknown): string {
  const code = typeof error === 'object' && error !== null && 'code' in error ? String(error.code) : '';
  const t = textosActuales(ERRORES_AUTH);
  const mensajes: Record<string, string> = t.codigos;
  return mensajes[code] ?? t.generico;
}
