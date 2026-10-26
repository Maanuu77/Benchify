import { HttpErrorResponse } from '@angular/common/http';
import { Component, input, model } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { BancosApi } from '../../../core/api/bancos-api';
import { Banco } from '../../../core/api/models';
import { AuthService } from '../../../core/auth/auth.service';
import { FakeAuthService } from '../../../core/auth/testing/fake-auth.service';
import { GeolocationService } from '../../../core/geo/geolocation.service';
import { Notifier } from '../../../core/ui/notifier';
import { Punto } from '../../../shared/map/bench-map';
import { LocationPicker, redondear } from '../../../shared/map/location-picker';
import { GaleriaImagenes } from '../detalle/galeria-imagenes';
import { BancoFormPage, maxEtiquetasValidator } from './banco-form-page';

@Component({ selector: 'bf-location-picker, bf-galeria-imagenes', template: '' })
class Stub {
  readonly ubicacion = model<Punto | null>(null);
  readonly centro = input<unknown>();
  readonly bancoId = input<unknown>();
  readonly tituloBanco = input<unknown>();
  readonly puedeSubir = input<unknown>();
}

const BANCO = { id: 'b1', titulo: 'Nuevo', estado: 'PENDIENTE' } as Banco;

describe('BancoFormPage', () => {
  let fixture: ComponentFixture<BancoFormPage>;
  let element: HTMLElement;
  let api: Record<'proponer' | 'editar' | 'obtener' | 'etiquetas', ReturnType<typeof vi.fn>>;
  let notifier: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    api = {
      proponer: vi.fn(() => of(BANCO)),
      editar: vi.fn(() => of(BANCO)),
      obtener: vi.fn(),
      etiquetas: vi.fn(() => of([{ id: 1, nombre: 'relax', color: '#3498DB', icono: 'spa' }])),
    };
    notifier = { success: vi.fn(), error: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: BancosApi, useValue: api },
        { provide: Notifier, useValue: notifier },
        { provide: AuthService, useValue: new FakeAuthService() },
        { provide: GeolocationService, useValue: { posicionActual: vi.fn() } },
      ],
    });
    TestBed.overrideComponent(BancoFormPage, {
      remove: { imports: [LocationPicker, GaleriaImagenes] }, add: { imports: [Stub] },
    });
  });

  async function crear(id?: string): Promise<void> {
    fixture = TestBed.createComponent(BancoFormPage);
    if (id) fixture.componentRef.setInput('id', id);
    element = fixture.nativeElement;
    await fixture.whenStable();
  }

  function escribir(nombre: string, valor: string): void {
    const input = element.querySelector<HTMLInputElement>(`[formcontrolname=${nombre}]`)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  function enviar(): Promise<void> {
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    return fixture.whenStable();
  }

  it('asks for a location before sending', async () => {
    await crear();
    escribir('titulo', 'Banco');
    await enviar();

    expect(api.proponer).not.toHaveBeenCalled();
    expect(notifier.error).toHaveBeenCalledWith('Marca en el mapa dónde está el banco.');
  });

  it('proposes a bench with trimmed text and nulls for empty fields, then offers photos', async () => {
    await crear();
    escribir('titulo', '  Banco nuevo ');
    escribir('lat', '37.4185');
    escribir('lng', '-5.9905');
    escribir('ciudad', 'Sevilla');
    await enviar();

    expect(api.proponer).toHaveBeenCalledWith({
      titulo: 'Banco nuevo', descripcion: null, lat: 37.4185, lng: -5.9905, direccion: null, ciudad: 'Sevilla',
      provincia: null, conSombra: false, accesible: false, conRespaldo: false, material: null, etiquetaIds: [],
    });
    expect(element.textContent).toContain('¡Gracias por añadir «Nuevo»!');
    expect(element.textContent).toContain('en cuanto lo revise un moderador');
  });

  it('shows backend field errors on the matching control', async () => {
    api.proponer.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400, error: { code: 'VALIDATION_FAILED', errors: [{ field: 'titulo', message: 'Título repetido' }] },
    })));
    await crear();
    escribir('titulo', 'Banco');
    escribir('lat', '37');
    escribir('lng', '-6');
    await enviar();
    fixture.detectChanges();

    expect(element.textContent).toContain('Título repetido');
  });

  it('loads the bench when editing and saves with PUT', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    api.obtener.mockReturnValue(of({
      ...BANCO, titulo: 'Existente', descripcion: null, lat: 37.1, lng: -6.1, direccion: null, ciudad: 'Sevilla',
      provincia: null, conSombra: true, accesible: false, conRespaldo: false, material: 'MADERA',
      etiquetas: [{ id: 1, nombre: 'relax', color: '#3498DB', icono: 'spa' }],
    }));
    await crear('b1');

    expect(element.querySelector('h1')?.textContent).toContain('Editar banco');
    expect(element.querySelector<HTMLInputElement>('[formcontrolname=titulo]')!.value).toBe('Existente');
    await enviar();

    expect(api.editar).toHaveBeenCalledWith('b1', expect.objectContaining({ titulo: 'Existente', etiquetaIds: [1] }));
    expect(navigate).toHaveBeenCalledWith(['/bancos', 'b1']);
  });
});

describe('form helpers', () => {
  it('rounds coordinates to six decimals', () => {
    expect(redondear({ lat: 37.123456789, lng: -5.987654321 })).toEqual({ lat: 37.123457, lng: -5.987654 });
  });

  it('limits tags to five', () => {
    expect(maxEtiquetasValidator(new FormControl([1, 2, 3, 4, 5]))).toBeNull();
    expect(maxEtiquetasValidator(new FormControl([1, 2, 3, 4, 5, 6]))).toEqual({ demasiadas: true });
  });
});
