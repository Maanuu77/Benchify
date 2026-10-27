import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { API_URL } from '../config/api-url';
import { AdminApi } from './admin-api';
import { BancosApi } from './bancos-api';
import { ResenasApi } from './resenas-api';
import { UsuariosApi } from './usuarios-api';

const API = 'http://api.test/api/v1';

describe('API clients', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: API }],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('queries the map area with filters as query params', async () => {
    const result = firstValueFrom(TestBed.inject(BancosApi)
      .enArea({ minLat: 37.3, minLng: -6.1, maxLat: 37.4, maxLng: -5.9 }, { accesible: true }));

    const request = http.expectOne((r) => r.url === `${API}/bancos`);
    expect(request.request.params.toString()).toBe('minLat=37.3&minLng=-6.1&maxLat=37.4&maxLng=-5.9&accesible=true');
    request.flush([]);
    expect(await result).toEqual([]);
  });

  it('searches nearby benches with radius and pagination', () => {
    TestBed.inject(BancosApi).cercanos(37.38, -6, 800, {}, { page: 1, size: 10 }).subscribe();

    const request = http.expectOne((r) => r.url === `${API}/bancos/cercanos`);
    expect(request.request.params.toString()).toBe('lat=37.38&lng=-6&radio=800&page=1&size=10');
    request.flush({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('caches the tag catalog for the session', () => {
    const api = TestBed.inject(BancosApi);
    api.etiquetas().subscribe();
    api.etiquetas().subscribe();

    http.expectOne(`${API}/etiquetas`).flush([{ id: 1, nombre: 'relax', color: '#3498DB', icono: 'spa' }]);
    api.etiquetas().subscribe();
    http.expectNone(`${API}/etiquetas`);
  });

  it('uploads images as multipart with the expected part name', () => {
    const file = new File([new Uint8Array([0x89, 0x50])], 'foto.png', { type: 'image/png' });
    TestBed.inject(BancosApi).subirImagen('b1', file).subscribe();

    const request = http.expectOne(`${API}/bancos/b1/imagenes`);
    expect(request.request.method).toBe('POST');
    expect((request.request.body as FormData).get('archivo')).toBeInstanceOf(File);
    request.flush({});
  });

  it('maps favourite state and check-in totals to plain values', async () => {
    const api = TestBed.inject(UsuariosApi);
    const favorito = firstValueFrom(api.esFavorito('b1'));
    const total = firstValueFrom(api.totalCheckIns('b1'));

    http.expectOne(`${API}/bancos/b1/favorito`).flush({ favorito: true });
    http.expectOne(`${API}/bancos/b1/checkins/total`).flush({ total: 7 });
    expect(await favorito).toBe(true);
    expect(await total).toBe(7);
  });

  it('uses the right verbs for favourites, reviews and moderation', () => {
    TestBed.inject(UsuariosApi).marcarFavorito('b1').subscribe();
    TestBed.inject(UsuariosApi).desmarcarFavorito('b1').subscribe();
    TestBed.inject(ResenasApi).reportar('r1').subscribe();
    TestBed.inject(AdminApi).aprobarBanco('b2').subscribe();
    TestBed.inject(AdminApi).cambiarRol('u1', 'ADMIN').subscribe();

    expect(http.expectOne({ method: 'PUT', url: `${API}/bancos/b1/favorito` })).toBeTruthy();
    expect(http.expectOne({ method: 'DELETE', url: `${API}/bancos/b1/favorito` })).toBeTruthy();
    expect(http.expectOne({ method: 'POST', url: `${API}/resenas/r1/reportes` })).toBeTruthy();
    expect(http.expectOne({ method: 'POST', url: `${API}/admin/bancos/b2/aprobacion` })).toBeTruthy();
    expect(http.expectOne({ method: 'PUT', url: `${API}/admin/usuarios/u1/rol` }).request.body).toEqual({ rol: 'ADMIN' });
  });
});
