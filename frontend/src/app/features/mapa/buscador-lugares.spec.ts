import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { LugaresApi } from '../../core/api/lugares-api';
import { Lugar } from '../../core/api/models';
import { BuscadorLugares } from './buscador-lugares';

const lugar = (nombre: string, extra: Partial<Lugar> = {}): Lugar => ({
  nombre, direccion: `${nombre}, Sevilla`, lat: 37.38, lng: -5.99, sur: null, oeste: null, norte: null, este: null, ...extra,
});

describe('BuscadorLugares', () => {
  let fixture: ComponentFixture<BuscadorLugares>;
  let element: HTMLElement;
  let api: { buscar: ReturnType<typeof vi.fn> };
  let elegidos: Lugar[];

  beforeEach(async () => {
    api = { buscar: vi.fn(() => of([lugar('Plaza de España'), lugar('Plaza Nueva')])) };
    TestBed.configureTestingModule({ providers: [{ provide: LugaresApi, useValue: api }] });
    fixture = TestBed.createComponent(BuscadorLugares);
    fixture.componentRef.setInput('cerca', { lat: 37.4, lng: -6, zoom: 14 });
    elegidos = [];
    fixture.componentInstance.lugarElegido.subscribe((l) => elegidos.push(l));
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  async function buscar(texto: string): Promise<void> {
    const input = element.querySelector('input')!;
    input.value = texto;
    input.dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('searches near the map centre and lets the user pick a result', async () => {
    await buscar('  plaza ');

    expect(api.buscar).toHaveBeenCalledWith('plaza', { lat: 37.4, lng: -6, zoom: 14 });
    const opciones = element.querySelectorAll<HTMLButtonElement>('li button');
    expect(opciones).toHaveLength(2);
    expect(element.textContent).toContain('OpenStreetMap');

    opciones[1].click();
    await fixture.whenStable();

    expect(elegidos.map((l) => l.nombre)).toEqual(['Plaza Nueva']);
    expect(element.querySelector('.resultados')).toBeNull();
    expect(element.querySelector('input')!.value).toBe('Plaza Nueva');
  });

  it('goes straight to a single result and ignores too short searches', async () => {
    await buscar('ab');
    expect(api.buscar).not.toHaveBeenCalled();

    api.buscar.mockReturnValue(of([lugar('Triana')]));
    await buscar('Triana');

    expect(elegidos.map((l) => l.nombre)).toEqual(['Triana']);
    expect(element.querySelector('.resultados')).toBeNull();
  });

  it('explains empty results and errors', async () => {
    api.buscar.mockReturnValue(of([]));
    await buscar('zzzz');
    expect(element.textContent).toContain('No hemos encontrado ese sitio');

    api.buscar.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 503, error: { detail: 'El buscador de direcciones no responde; prueba más tarde', code: 'GEOCODIFICACION_NO_DISPONIBLE' },
    })));
    await buscar('Macarena');
    expect(element.textContent).toContain('El buscador de direcciones no responde');
  });

  it('clears the search and closes the results', async () => {
    await buscar('plaza');
    element.querySelector<HTMLButtonElement>('.limpiar')!.click();
    await fixture.whenStable();

    expect(element.querySelector('input')!.value).toBe('');
    expect(element.querySelector('.resultados')).toBeNull();
  });
});
