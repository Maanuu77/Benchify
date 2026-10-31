import type * as Leaflet from 'leaflet';

export type LeafletLib = typeof Leaflet;

let cargado: Promise<LeafletLib> | null = null;

/**
 * Carga Leaflet, el plugin de clustering y sus estilos bajo demanda, solo en las pantallas con mapa.
 * El plugin se registra sobre el objeto global `L`, así que se expone antes de importarlo.
 */
export function cargarLeaflet(): Promise<LeafletLib> {
  cargado ??= (async () => {
    cargarEstilos();
    const modulo = await import('leaflet');
    const L = ((modulo as { default?: LeafletLib }).default ?? modulo) as LeafletLib;
    (globalThis as { L?: LeafletLib }).L = L;
    await import('leaflet.markercluster');
    return L;
  })();
  return cargado;
}

/** leaflet.css se compila como bundle aparte (angular.json, inject: false) y se enlaza aquí. */
function cargarEstilos(): void {
  if (typeof document === 'undefined' || document.querySelector('link[data-leaflet]')) {
    return;
  }
  const link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = 'leaflet.css';
  link.dataset['leaflet'] = '';
  document.head.appendChild(link);
}

/**
 * Teselas estándar de OpenStreetMap (sin clave de API). El estilo minimal (gris en claro, invertido
 * en oscuro) se consigue con un filtro CSS sobre la capa, en styles.scss (.bf-teselas).
 */
export const TILE_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
export const TILE_ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

/** Añade la capa base al mapa. Devuelve la función que la quita. */
export function anadirCapaBase(L: LeafletLib, map: Leaflet.Map): () => void {
  const capa = L.tileLayer(TILE_URL, {
    attribution: TILE_ATTRIBUTION,
    maxZoom: 19,
    className: 'bf-teselas',
  }).addTo(map);
  return () => capa.remove();
}

/** Silueta de banco de la marca (24×24), en línea para no depender de imágenes que el bundler no resuelve. */
export const SVG_BANCO =
  '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="4.5" width="14" height="4" rx="2"/>' +
  '<rect x="3" y="10.5" width="18" height="4" rx="2"/><rect x="5.5" y="14" width="3" height="6" rx="1"/>' +
  '<rect x="15.5" y="14" width="3" height="6" rx="1"/></svg>';

/** Marcador de banco; el seleccionado crece y pasa a forma de chincheta. */
export function iconoBanco(L: LeafletLib, seleccionado = false): Leaflet.DivIcon {
  return L.divIcon({
    className: '',
    html: `<div class="bf-pin${seleccionado ? ' bf-pin--selected' : ''}">${SVG_BANCO}</div>`,
    iconSize: seleccionado ? [46, 46] : [30, 30],
    iconAnchor: seleccionado ? [23, 52] : [15, 15],
  });
}

/** Agrupación: círculo con el número de bancos, más grande cuantos más agrupa. */
export function iconoGrupo(L: LeafletLib, cluster: Leaflet.MarkerCluster): Leaflet.DivIcon {
  const total = cluster.getChildCount();
  const tamano = total < 10 ? 38 : total < 100 ? 44 : 52;
  return L.divIcon({
    className: '',
    html: `<div class="bf-cluster" style="width:${tamano}px;height:${tamano}px">${total}</div>`,
    iconSize: [tamano, tamano],
  });
}

export function iconoUsuario(L: LeafletLib): Leaflet.DivIcon {
  return L.divIcon({ className: '', html: '<div class="bf-yo"></div>', iconSize: [20, 20] });
}
