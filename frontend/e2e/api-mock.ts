import { Page, Route } from '@playwright/test';

export const BANCOS = [
  {
    id: 'b-torre', titulo: 'Banco frente a la Torre del Oro', lat: 37.3825, lng: -5.996, ciudad: 'Sevilla',
    estado: 'PUBLICADO', puntuacionMedia: 4.8, numResenas: 12, accesible: true, conSombra: false, conRespaldo: true,
    etiquetas: ['vistas'],
  },
  {
    id: 'b-glorieta', titulo: 'Glorieta de Bécquer', lat: 37.3763, lng: -5.9892, ciudad: 'Sevilla',
    estado: 'PUBLICADO', puntuacionMedia: 4.1, numResenas: 5, accesible: false, conSombra: true, conRespaldo: true,
    etiquetas: ['sombra'],
  },
];

const DETALLE = {
  ...BANCOS[0], descripcion: 'Vistas al Guadalquivir.', direccion: 'Paseo de Cristóbal Colón', provincia: 'Sevilla',
  pais: 'España', material: 'MADERA', autor: { id: 'dev-admin', nombre: 'Admin Benchify' },
  etiquetas: [{ id: 3, nombre: 'vistas', color: '#2ECC71', icono: 'landscape' }],
  fechaSubida: '2026-09-25T10:00:00Z', fechaVerificacion: '2026-09-26T10:00:00Z',
};

const page = <T>(content: T[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 });

/** Simula la API de Benchify y bloquea las teselas externas para que las pruebas no dependan de la red. */
export async function mockApi(p: Page): Promise<string[]> {
  const peticiones: string[] = [];
  await p.route('**/tile.openstreetmap.org/**', (route) => route.fulfill({ status: 204 }));
  await p.route('**/api/v1/**', async (route: Route) => {
    const url = new URL(route.request().url());
    peticiones.push(url.pathname + url.search);
    const path = url.pathname.replace(/^.*\/api\/v1/, '');
    const json = (body: unknown, status = 200) => route.fulfill({ status, json: body });

    if (path === '/etiquetas') return json([{ id: 3, nombre: 'vistas', color: '#2ECC71', icono: 'landscape' },
      { id: 4, nombre: 'sombra', color: '#7F8C8D', icono: 'park' }]);
    if (path === '/bancos') {
      const soloAccesibles = url.searchParams.get('accesible') === 'true';
      return json(BANCOS.filter((b) => !soloAccesibles || b.accesible));
    }
    if (path === '/bancos/b-torre') return json(DETALLE);
    if (path === '/bancos/b-torre/imagenes') return json([]);
    if (path === '/bancos/b-torre/resenas') return json(page([{
      id: 'r1', bancoId: 'b-torre', autorId: 'dev-user', autorNombre: 'Ana García', puntuacion: 5,
      comentario: 'Las mejores vistas del río.', fecha: '2026-10-01T10:00:00Z', fechaEdicion: null,
    }]));
    if (path === '/bancos/b-torre/checkins/total') return json({ total: 3 });
    if (path.startsWith('/bancos/')) return json({ status: 404, code: 'BANCO_NOT_FOUND', detail: 'Banco no encontrado' }, 404);
    return json({ status: 404, code: 'NOT_FOUND' }, 404);
  });
  return peticiones;
}
