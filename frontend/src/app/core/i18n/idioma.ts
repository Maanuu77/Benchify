import { inject, InjectionToken } from '@angular/core';

export type Idioma = 'es' | 'en';

export const IDIOMAS: readonly Idioma[] = ['es', 'en'];

/** Locale de Angular (fechas y números) de cada idioma. */
export const LOCALES: Record<Idioma, string> = { es: 'es-ES', en: 'en-GB' };

const CLAVE = 'bf-idioma';

/**
 * Idioma elegido a mano o, si no, el primero del navegador que la aplicación entiende; español por
 * defecto. Se decide al arrancar: cambiarlo recarga la página (ver {@link cambiarIdioma}), así que
 * durante una visita no cambia y los textos, las fechas y los números siempre coinciden.
 */
export function idiomaPreferido(): Idioma {
  try {
    const guardado = localStorage.getItem(CLAVE);
    if (guardado === 'es' || guardado === 'en') {
      return guardado;
    }
  } catch {
    // Sin almacenamiento se decide por el navegador
  }
  const lista = navigator.languages?.length ? navigator.languages : navigator.language ? [navigator.language] : [];
  const delNavegador = lista
    .map((l) => l.slice(0, 2).toLowerCase())
    .find((l): l is Idioma => l === 'es' || l === 'en');
  return delNavegador ?? 'es';
}

let actual: Idioma | null = null;

/** El idioma de esta visita, para el código que no tiene inyección de dependencias. */
export function idiomaActual(): Idioma {
  actual ??= idiomaPreferido();
  return actual;
}

export const IDIOMA = new InjectionToken<Idioma>('IDIOMA', { providedIn: 'root', factory: idiomaActual });

/** Guarda la preferencia y recarga para aplicar el idioma en toda la aplicación. */
export function cambiarIdioma(idioma: Idioma): void {
  try {
    localStorage.setItem(CLAVE, idioma);
  } catch {
    // Sin almacenamiento el cambio dura solo hasta que se cierre la pestaña: no hay forma de recordarlo
  }
  location.reload();
}

/**
 * Lo que debe tener la traducción de unos textos: las mismas claves, con cadenas donde el original
 * tiene cadenas y funciones con los mismos parámetros donde tiene funciones (plurales, valores).
 */
export type Traduccion<T> = {
  [K in keyof T]: T[K] extends (...args: infer A) => string
    ? (...args: A) => string
    : T[K] extends string
      ? string
      : Traduccion<T[K]>;
};

/** Textos de una parte de la aplicación en todos los idiomas; el compilador exige que `en` esté completo. */
export function traducciones<T>(es: T, en: Traduccion<T>): Record<Idioma, T> {
  return { es, en: en as T };
}

/**
 * Los textos en el idioma de la visita. Dentro de un contexto de inyección (componentes, servicios,
 * resolvers) usa el token, que los tests pueden sustituir; fuera, el idioma detectado al arrancar.
 */
export function textos<T>(definicion: Record<Idioma, T>): T {
  const idioma = inject(IDIOMA, { optional: true }) ?? idiomaActual();
  return definicion[idioma];
}

/** Como {@link textos}, para funciones que se llaman fuera de un contexto de inyección. */
export function textosActuales<T>(definicion: Record<Idioma, T>): T {
  return definicion[idiomaActual()];
}
