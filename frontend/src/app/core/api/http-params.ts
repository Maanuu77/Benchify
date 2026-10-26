import { HttpParams } from '@angular/common/http';

type ParamValue = string | number | boolean | null | undefined;

/** HttpParams sin los valores vacíos, para que los filtros opcionales no se envíen. */
export function params(values: Record<string, ParamValue>): HttpParams {
  let result = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== null && value !== undefined && value !== '') {
      result = result.set(key, String(value));
    }
  }
  return result;
}
