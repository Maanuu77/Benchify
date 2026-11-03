import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config/api-url';
import { IDIOMA, idiomaPreferido, textos, traducciones } from './idioma';
import { idiomaInterceptor } from './idioma.interceptor';

const SALUDO = traducciones(
  { hola: 'Hola', bancos: (n: number) => `${n} bancos` },
  { hola: 'Hello', bancos: (n) => `${n} benches` },
);

describe('idioma', () => {
  afterEach(() => {
    localStorage.clear();
    Object.defineProperty(navigator, 'languages', { value: ['es-ES'], configurable: true });
  });

  it('prefers the saved choice, then the browser, then Spanish', () => {
    expect(idiomaPreferido()).toBe('es');

    Object.defineProperty(navigator, 'languages', { value: ['fr-FR', 'en-US', 'es-ES'], configurable: true });
    expect(idiomaPreferido()).toBe('en');

    Object.defineProperty(navigator, 'languages', { value: ['de-DE'], configurable: true });
    expect(idiomaPreferido()).toBe('es');

    localStorage.setItem('bf-idioma', 'en');
    expect(idiomaPreferido()).toBe('en');
    localStorage.setItem('bf-idioma', 'xx');
    expect(idiomaPreferido()).toBe('es');
  });

  it('picks the texts of the current language', () => {
    TestBed.configureTestingModule({ providers: [{ provide: IDIOMA, useValue: 'en' }] });

    const t = TestBed.runInInjectionContext(() => textos(SALUDO));

    expect(t.hola).toBe('Hello');
    expect(t.bancos(3)).toBe('3 benches');
  });

  it('asks the API for responses in the chosen language, and only the API', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([idiomaInterceptor])),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: 'http://api.test/api/v1' },
        { provide: IDIOMA, useValue: 'en' },
      ],
    });
    const http = TestBed.inject(HttpClient);
    const backend = TestBed.inject(HttpTestingController);

    http.get('http://api.test/api/v1/lugares').subscribe();
    http.get('https://tile.openstreetmap.org/1/1/1.png').subscribe();

    expect(backend.expectOne('http://api.test/api/v1/lugares').request.headers.get('Accept-Language')).toBe('en');
    expect(backend.expectOne('https://tile.openstreetmap.org/1/1/1.png').request.headers.has('Accept-Language')).toBe(false);
  });
});
