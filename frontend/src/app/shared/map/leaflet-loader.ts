import type * as Leaflet from 'leaflet';

export type LeafletLib = typeof Leaflet;

let cargado: Promise<LeafletLib> | null = null;

/**
 * Carga Leaflet y el plugin de clustering bajo demanda, solo en las pantallas con mapa.
 * El plugin se registra sobre el objeto global `L`, así que se expone antes de importarlo.
 */
export function cargarLeaflet(): Promise<LeafletLib> {
  cargado ??= (async () => {
    const modulo = await import('leaflet');
    const L = ((modulo as { default?: LeafletLib }).default ?? modulo) as LeafletLib;
    (globalThis as { L?: LeafletLib }).L = L;
    await import('leaflet.markercluster');
    return L;
  })();
  return cargado;
}

export const TILE_URL = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
export const TILE_ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

/** Marcador propio (sin imágenes de Leaflet, que el bundler no resuelve bien). */
export function iconoBanco(L: LeafletLib, seleccionado = false): Leaflet.DivIcon {
  return L.divIcon({
    className: 'bf-marker' + (seleccionado ? ' bf-marker--selected' : ''),
    html: '<span class="material-icons" aria-hidden="true">event_seat</span>',
    iconSize: [36, 36],
    iconAnchor: [18, 34],
    popupAnchor: [0, -30],
  });
}
