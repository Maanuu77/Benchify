import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { IncidenciasApi } from '../../core/api/incidencias-api';
import { Incidencia, Page } from '../../core/api/models';
import { Notifier } from '../../core/ui/notifier';
import { ColaIncidencias } from './cola-incidencias';

const pagina = <T>(content: T[]): Page<T> => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 });

describe('ColaIncidencias', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    api = {
      abiertas: vi.fn(() => of(pagina<Incidencia>([
        { id: 'n1', bancoId: 'b1', usuarioId: 'ana', tipo: 'ROTO', comentario: 'Tabla suelta', estado: 'ABIERTA', fecha: '2026-10-01' },
        { id: 'n2', bancoId: 'b2', usuarioId: 'luis', tipo: 'SUCIO', comentario: null, estado: 'ABIERTA', fecha: '2026-10-02' },
      ]))),
      cerrar: vi.fn(() => of({})),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: IncidenciasApi, useValue: api },
        { provide: Notifier, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    });
  });

  it('resolves or dismisses open reports', async () => {
    const fixture = TestBed.createComponent(ColaIncidencias);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Avisos de la comunidad (2)');
    expect(element.textContent).toContain('Está roto');
    expect(element.textContent).toContain('(sin detalles)');

    const boton = (texto: string) =>
      Array.from(element.querySelectorAll('button')).find((b) => b.textContent?.includes(texto))!;
    boton('Resuelta').click();
    await fixture.whenStable();
    expect(api['cerrar']).toHaveBeenCalledWith('n1', 'RESUELTA');

    boton('Descartar').click();
    await fixture.whenStable();
    expect(api['cerrar']).toHaveBeenCalledWith('n2', 'DESCARTADA');
    expect(element.textContent).toContain('No hay avisos abiertos.');
  });
});
