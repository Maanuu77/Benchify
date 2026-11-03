import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { API_URL } from '../config/api-url';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { FakeAuthService } from './testing/fake-auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let auth: FakeAuthService;

  beforeEach(() => {
    auth = new FakeAuthService();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: 'http://api.test/api/v1' },
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  async function flushNext(url: string) {
    // El token se obtiene de forma asíncrona antes de enviar la petición
    await new Promise((resolve) => setTimeout(resolve));
    return controller.expectOne(url);
  }

  it('adds the bearer token to API calls when signed in', async () => {
    auth.signIn();
    const response = firstValueFrom(http.get('http://api.test/api/v1/usuarios/me'));

    const request = await flushNext('http://api.test/api/v1/usuarios/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-ana');
    request.flush({});
    await response;
  });

  it('sends anonymous API calls without the header', async () => {
    const response = firstValueFrom(http.get('http://api.test/api/v1/bancos'));

    const request = await flushNext('http://api.test/api/v1/bancos');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush([]);
    await response;
  });

  it('retries once with a fresh token when the API rejects the current one', async () => {
    auth.signIn();
    const response = firstValueFrom(http.get('http://api.test/api/v1/admin/estadisticas'));

    (await flushNext('http://api.test/api/v1/admin/estadisticas')).flush(null, { status: 403, statusText: 'Forbidden' });
    const retry = await flushNext('http://api.test/api/v1/admin/estadisticas');
    expect(auth.forcedRefreshes).toBe(1);
    expect(retry.request.headers.get('Authorization')).toBe('Bearer token-ana');
    retry.flush({ ok: true });
    await expect(response).resolves.toEqual({ ok: true });
  });

  it('gives up after the single retry', async () => {
    auth.signIn();
    const response = firstValueFrom(http.delete('http://api.test/api/v1/imagenes/1'));

    (await flushNext('http://api.test/api/v1/imagenes/1')).flush(null, { status: 403, statusText: 'Forbidden' });
    (await flushNext('http://api.test/api/v1/imagenes/1')).flush(null, { status: 403, statusText: 'Forbidden' });
    await expect(response).rejects.toMatchObject({ status: 403 });
    expect(auth.forcedRefreshes).toBe(1);
  });

  it('does not retry anonymous calls or other errors', async () => {
    const anonymous = firstValueFrom(http.get('http://api.test/api/v1/usuarios/me'));
    (await flushNext('http://api.test/api/v1/usuarios/me')).flush(null, { status: 401, statusText: 'Unauthorized' });
    await expect(anonymous).rejects.toMatchObject({ status: 401 });

    auth.signIn();
    const missing = firstValueFrom(http.get('http://api.test/api/v1/bancos/x'));
    (await flushNext('http://api.test/api/v1/bancos/x')).flush(null, { status: 404, statusText: 'Not Found' });
    await expect(missing).rejects.toMatchObject({ status: 404 });
    expect(auth.forcedRefreshes).toBe(0);
  });

  it('never leaks the token to other hosts', async () => {
    auth.signIn();
    const response = firstValueFrom(http.get('https://tile.openstreetmap.org/1/1/1.png'));

    const request = controller.expectOne('https://tile.openstreetmap.org/1/1/1.png');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush('');
    await response;
  });
});
