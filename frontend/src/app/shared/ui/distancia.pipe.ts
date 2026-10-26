import { Pipe, PipeTransform } from '@angular/core';

/** 230 → "230 m", 1520 → "1,5 km". */
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
    return `${km.toLocaleString('es-ES', { maximumFractionDigits: km < 10 ? 1 : 0 })} km`;
  }
}
