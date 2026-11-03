import { Pipe, PipeTransform } from '@angular/core';
import { idiomaActual, LOCALES } from '../../core/i18n/idioma';

/**
 * 230 → "230 m", 1520 → "1,5 km" (o "1.5 km" en inglés). Se crea también con `new` fuera de un
 * contexto de inyección, así que toma el idioma detectado al arrancar.
 */
@Pipe({ name: 'distancia' })
export class DistanciaPipe implements PipeTransform {
  transform(metros: number | null | undefined): string {
    if (metros === null || metros === undefined || Number.isNaN(metros)) {
      return '';
    }
    if (metros < 1000) {
      return `${Math.round(metros)} m`;
    }
    const km = metros / 1000;
    return `${km.toLocaleString(LOCALES[idiomaActual()], { maximumFractionDigits: km < 10 ? 1 : 0 })} km`;
  }
}
