import { Component, input } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { BancosApi } from '../../../core/api/bancos-api';
import { Banco } from '../../../core/api/models';
import { UsuariosApi } from '../../../core/api/usuarios-api';
import { AuthService } from '../../../core/auth/auth.service';
import { FakeAuthService } from '../../../core/auth/testing/fake-auth.service';
import { GeolocationService } from '../../../core/geo/geolocation.service';
import { Notifier } from '../../../core/ui/notifier';
import { BenchMap } from '../../../shared/map/bench-map';
import { BancoDetallePage } from './banco-detalle-page';
import { GaleriaImagenes, validarImagen } from './galeria-imagenes';
import { ResenasSection } from './resenas-section';

@Component({ selector: 'bf-bench-map, bf-galeria-imagenes, bf-resenas-section', template: '' })
class Stub {
  readonly bancos = input<unknown>();
  readonly centro = input<unknown>();
  readonly zoom = input<unknown>();
  readonly ariaLabel = input<unknown>();
  readonly bancoId = input<unknown>();
  readonly tituloBanco = input<unknown>();
  readonly puedeSubir = input<unknown>();
  readonly autorBancoId = input<unknown>();
  readonly publicado = input<unknown>();
}

const BANCO: Banco = {
  id: 'b1', titulo: 'Glorieta de Bécquer', descripcion: 'Con sombra', lat: 37.37, lng: -5.98,
  direccion: 'Parque de María Luisa', ciudad: 'Sevilla', provincia: null, pais: 'España', estado: 'PUBLICADO',
  conSombra: true, accesible: false, conRespaldo: true, material: 'PIEDRA', puntuacionMedia: 4.3, numResenas: 3,
  autor: { id: 'autor', nombre: 'Autora' }, etiquetas: [{ id: 1, nombre: 'sombra', color: '#7F8C8D', icono: 'park' }],
  fechaSubida: '2026-10-01T10:00:00Z', fechaVerificacion: '2026-10-02T10:00:00Z',
};

describe('BancoDetallePage', () => {
  let fixture: ComponentFixture<BancoDetallePage>;
  let element: HTMLElement;
  let auth: FakeAuthService;
  let bancos: { obtener: ReturnType<typeof vi.fn>; eliminar: ReturnType<typeof vi.fn> };
  let usuarios: Record<string, ReturnType<typeof vi.fn>>;
  let notifier: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  let geo: { posicionActual: ReturnType<typeof vi.fn> };

  async function crear(): Promise<void> {
    fixture = TestBed.createComponent(BancoDetallePage);
    fixture.componentRef.setInput('id', 'b1');
    element = fixture.nativeElement;
    await fixture.whenStable();
  }

  beforeEach(() => {
    auth = new FakeAuthService();
    bancos = { obtener: vi.fn(() => of(BANCO)), eliminar: vi.fn(() => of(undefined)) };
    usuarios = {
      esFavorito: vi.fn(() => of(false)), totalCheckIns: vi.fn(() => of(2)),
      marcarFavorito: vi.fn(() => of(undefined)), desmarcarFavorito: vi.fn(() => of(undefined)),
      checkIn: vi.fn(() => of({ distanciaMetros: 12 })),
    };
    notifier = { success: vi.fn(), error: vi.fn() };
    geo = { posicionActual: vi.fn(async () => ({ lat: 37.37, lng: -5.98, precision: 5 })) };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: BancosApi, useValue: bancos },
        { provide: UsuariosApi, useValue: usuarios },
        { provide: Notifier, useValue: notifier },
        { provide: GeolocationService, useValue: geo },
      ],
    });
    TestBed.overrideComponent(BancoDetallePage, {
      remove: { imports: [BenchMap, GaleriaImagenes, ResenasSection] },
      add: { imports: [Stub] },
    });
  });

  it('renders the bench data and features', async () => {
    await crear();

    expect(element.querySelector('h1')?.textContent).toContain('Glorieta de Bécquer');
    expect(element.textContent).toContain('Parque de María Luisa, Sevilla');
    expect(element.textContent).toContain('3 reseñas');
    expect(element.textContent).toContain('2 check-ins');
    expect(element.textContent).toContain('Con sombra');
    expect(element.textContent).toContain('#sombra');
    expect(element.textContent).not.toContain('Eliminar');
  });

  it('refreshes the bench without unmounting the page', async () => {
    await crear();
    const cabecera = element.querySelector('h1');
    bancos.obtener.mockReturnValue(of({ ...BANCO, numResenas: 4 }));

    fixture.componentInstance['recargar']();
    await fixture.whenStable();

    expect(element.querySelector('h1')).toBe(cabecera);
    expect(element.textContent).toContain('4 reseñas');
  });

  it('reloads the bench and the favourite when the user signs in', async () => {
    usuarios['esFavorito'].mockReturnValue(of(true));
    await crear();
    expect(usuarios['esFavorito']).not.toHaveBeenCalled();

    auth.signIn();
    await fixture.whenStable();

    expect(bancos.obtener).toHaveBeenCalledTimes(2);
    expect(usuarios['esFavorito']).toHaveBeenCalledWith('b1');
    expect(element.querySelector('[aria-pressed]')?.getAttribute('aria-pressed')).toBe('true');
  });

  it('shows a not found page for a 404', async () => {
    bancos.obtener.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    await crear();

    expect(element.querySelector('h1')?.textContent).toContain('Banco no encontrado');
  });

  it('sends anonymous users to login when saving a favourite', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await crear();

    (element.querySelector('[aria-pressed]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/bancos/b1' } });
  });

  it('reverts the optimistic favourite when the API fails', async () => {
    auth.signIn();
    usuarios['marcarFavorito'].mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await crear();

    (element.querySelector('[aria-pressed]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(element.querySelector('[aria-pressed]')?.getAttribute('aria-pressed')).toBe('false');
    expect(notifier.error).toHaveBeenCalled();
  });

  it('checks in with the device position', async () => {
    auth.signIn();
    await crear();

    Array.from(element.querySelectorAll('button')).find((b) => b.textContent?.includes('Estoy aquí'))!.click();
    await fixture.whenStable();

    expect(usuarios['checkIn']).toHaveBeenCalledWith('b1', 37.37, -5.98);
    expect(element.textContent).toContain('3 check-ins');
    expect(notifier.success).toHaveBeenCalledWith('¡Check-in hecho! Estabas a 12 m.');
  });

  it('lets the author manage the bench', async () => {
    auth.signIn({ uid: 'autor' });
    await crear();

    expect(element.textContent).toContain('Editar');
    expect(element.textContent).toContain('Eliminar');
  });
});

describe('validarImagen', () => {
  it('accepts supported images up to 5 MB and rejects the rest', () => {
    expect(validarImagen(new File([new Uint8Array(10)], 'a.jpg', { type: 'image/jpeg' }))).toBeNull();
    expect(validarImagen(new File(['x'], 'a.gif', { type: 'image/gif' }))).toContain('JPEG, PNG o WebP');
    expect(validarImagen(new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'a.png', { type: 'image/png' })))
      .toContain('5 MB');
  });
});
