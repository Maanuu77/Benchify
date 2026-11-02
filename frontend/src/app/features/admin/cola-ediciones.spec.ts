import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { DatosBancoModeracion, EdicionModeracion, Page } from '../../core/api/models';
import { Notifier } from '../../core/ui/notifier';
import { Confirmation } from '../../shared/ui/confirm-dialog';
import { cambios, ColaEdiciones } from './cola-ediciones';

const pagina = <T>(content: T[]): Page<T> => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 });

const ACTUAL: DatosBancoModeracion = {
  titulo: 'Glorieta', descripcion: null, lat: 37.38, lng: -5.99, direccion: null, ciudad: 'Sevilla', provincia: null,
  pais: 'España', conSombra: false, accesible: true, conRespaldo: true, material: 'MADERA', etiquetas: ['relax'],
};

const edicion = (id: string, propuesta: Partial<DatosBancoModeracion>): EdicionModeracion => ({
  id, bancoId: 'b-' + id, autorId: 'ana', autorNombre: 'Ana', fecha: '2026-11-01T10:00:00Z',
  actual: ACTUAL, propuesta: { ...ACTUAL, ...propuesta },
});

describe('ColaEdiciones', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let confirmation: { pedirMotivo: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    api = {
      ediciones: vi.fn(() => of(pagina([
        edicion('e1', { titulo: 'Glorieta de Bécquer', conSombra: true, etiquetas: ['relax', 'vistas'] }),
        edicion('e2', { material: 'METAL' }),
      ]))),
      aprobarEdicion: vi.fn(() => of({})),
      rechazarEdicion: vi.fn(() => of(undefined)),
    };
    confirmation = { pedirMotivo: vi.fn(async () => 'Ubicación equivocada') };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AdminApi, useValue: api },
        { provide: Confirmation, useValue: confirmation },
        { provide: Notifier, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    });
  });

  it('lists only the fields an edit changes', () => {
    expect(cambios(edicion('x', { titulo: 'Nuevo', accesible: false }))).toEqual([
      { campo: 'Título', antes: 'Glorieta', despues: 'Nuevo' },
      { campo: 'Accesible', antes: 'Sí', despues: 'No' },
    ]);
    expect(cambios(edicion('x', {}))).toEqual([]);
  });

  it('shows the differences and applies or discards each edit', async () => {
    const fixture = TestBed.createComponent(ColaEdiciones);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Ediciones pendientes (2)');
    const filas = Array.from(element.querySelectorAll('li:first-child tbody tr')).map((tr) => tr.textContent?.trim());
    expect(filas).toEqual([
      'TítuloGlorietaGlorieta de Bécquer',
      'SombraNoSí',
      'Etiquetasrelaxrelax, vistas',
    ]);
    expect(element.textContent).toContain('MaderaMetal');

    const boton = (texto: string) =>
      Array.from(element.querySelectorAll('button')).find((b) => b.textContent?.includes(texto))!;
    boton('Aplicar').click();
    await fixture.whenStable();
    expect(api['aprobarEdicion']).toHaveBeenCalledWith('e1');

    boton('Descartar').click();
    await fixture.whenStable();
    expect(confirmation.pedirMotivo).toHaveBeenCalledWith(expect.objectContaining({ titulo: 'Descartar edición' }));
    expect(api['rechazarEdicion']).toHaveBeenCalledWith('e2', 'Ubicación equivocada');
    expect(element.textContent).toContain('No hay ediciones pendientes.');
  });
});
