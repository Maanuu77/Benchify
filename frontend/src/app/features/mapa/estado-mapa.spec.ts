import { aQueryParams, leerEstadoMapa } from './estado-mapa';

describe('estado del mapa en la URL', () => {
  it('reads the view and filters from the query params', () => {
    expect(leerEstadoMapa({ lat: '37.38', lng: '-5.98', z: '15', accesible: '1', respaldo: '1', etiqueta: '3', min: '4.5' }))
      .toEqual({
        vista: { lat: 37.38, lng: -5.98, zoom: 15 },
        filtro: { accesible: true, conRespaldo: true, etiqueta: 3, puntuacionMin: 4.5 },
      });
  });

  it('ignores missing or out-of-range values', () => {
    expect(leerEstadoMapa({ lat: '95', lng: '-5.98', z: '15', min: 'mucho', sombra: 'si' }))
      .toEqual({ vista: null, filtro: {} });
    expect(leerEstadoMapa({})).toEqual({ vista: null, filtro: {} });
  });

  it('writes compact params and clears the ones that do not apply', () => {
    expect(aQueryParams({ vista: { lat: 37.388601234, lng: -5.9823, zoom: 14.6 }, filtro: { conSombra: true } }))
      .toEqual({
        lat: '37.38860', lng: '-5.98230', z: 15,
        accesible: null, sombra: 1, respaldo: null, etiqueta: null, min: null,
      });
  });

  it('round-trips', () => {
    const estado = { vista: { lat: 37.5, lng: -6, zoom: 12 }, filtro: { accesible: true, puntuacionMin: 4 } };
    const params = Object.fromEntries(
      Object.entries(aQueryParams(estado)).filter(([, v]) => v != null).map(([k, v]) => [k, String(v)]),
    );

    expect(leerEstadoMapa(params)).toEqual(estado);
  });
});
