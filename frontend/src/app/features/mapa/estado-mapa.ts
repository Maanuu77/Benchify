import { Params } from '@angular/router';
import { FiltroBancos } from '../../core/api/models';
import { Vista, ZOOM_MINIMO } from '../../shared/map/bench-map';

/**
 * Vista y filtros del mapa en la URL (?lat=&lng=&z=&accesible=1…): al volver desde una ficha el
 * mapa queda como estaba y un enlace abre la misma zona con los mismos filtros.
 */
export interface EstadoMapa {
  vista: Vista | null;
  filtro: FiltroBancos;
}

export function leerEstadoMapa(params: Params): EstadoMapa {
  const lat = numero(params['lat'], -90, 90);
  const lng = numero(params['lng'], -180, 180);
  const zoom = numero(params['z'], ZOOM_MINIMO, 20);
  const filtro: FiltroBancos = {};
  if (params['accesible'] === '1') filtro.accesible = true;
  if (params['sombra'] === '1') filtro.conSombra = true;
  if (params['respaldo'] === '1') filtro.conRespaldo = true;
  const etiqueta = numero(params['etiqueta'], 1, Number.MAX_SAFE_INTEGER);
  if (etiqueta != null) filtro.etiqueta = Math.trunc(etiqueta);
  const puntuacionMin = numero(params['min'], 0, 5);
  if (puntuacionMin != null) filtro.puntuacionMin = puntuacionMin;
  return { vista: lat != null && lng != null && zoom != null ? { lat, lng, zoom } : null, filtro };
}

/** Parámetros de la URL; los que no aplican van a null para que el router los quite. */
export function aQueryParams({ vista, filtro }: EstadoMapa): Params {
  return {
    lat: vista ? vista.lat.toFixed(5) : null,
    lng: vista ? vista.lng.toFixed(5) : null,
    z: vista ? Math.round(vista.zoom) : null,
    accesible: filtro.accesible ? 1 : null,
    sombra: filtro.conSombra ? 1 : null,
    respaldo: filtro.conRespaldo ? 1 : null,
    etiqueta: filtro.etiqueta ?? null,
    min: filtro.puntuacionMin ?? null,
  };
}

function numero(valor: unknown, minimo: number, maximo: number): number | null {
  if (typeof valor !== 'string' || valor.trim() === '') return null;
  const n = Number(valor);
  return Number.isFinite(n) && n >= minimo && n <= maximo ? n : null;
}
