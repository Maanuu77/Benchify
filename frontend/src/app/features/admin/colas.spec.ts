import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { ImagenModeracion, Page, ResenaModeracion } from '../../core/api/models';
import { Notifier } from '../../core/ui/notifier';
import { Confirmation } from '../../shared/ui/confirm-dialog';
import { ColaImagenes } from './cola-imagenes';
import { ColaResenas } from './cola-resenas';

registerLocaleData(localeEs);

const pagina = <T>(content: T[]): Page<T> => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 });

describe('review and photo queues', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let confirmation: { ask: ReturnType<typeof vi.fn>; pedirMotivo: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    api = {
      resenasReportadas: vi.fn(() => of(pagina<ResenaModeracion>([
        { id: 'r1', bancoId: 'b1', autorId: 'luis', autorNombre: 'Luis', puntuacion: 1, comentario: 'spam', numReportes: 3, fecha: '2026-10-01' },
        { id: 'r2', bancoId: 'b1', autorId: 'eva', autorNombre: 'Eva', puntuacion: 2, comentario: null, numReportes: 4, fecha: '2026-10-02' },
      ]))),
      mantenerResena: vi.fn(() => of({})),
      eliminarResena: vi.fn(() => of(undefined)),
      imagenesPendientes: vi.fn(() => of(pagina<ImagenModeracion>([
        { id: 'i1', bancoId: 'b1', url: 'https://s/1.jpg', thumbUrl: 'https://s/1_thumb.jpg', requiereAutenticacion: false, estado: 'PENDIENTE', subidaPor: 'ana', tamanoBytes: 73, fechaSubida: '2026-10-01',
          parecidas: [
            { id: 'p1', bancoId: 'b1', thumbUrl: 'https://s/p1_thumb.jpg', requiereAutenticacion: false, estado: 'APROBADA', mismoBanco: true },
            { id: 'p2', bancoId: 'b9', thumbUrl: null, requiereAutenticacion: false, estado: 'RECHAZADA', mismoBanco: false },
          ] },
        { id: 'i2', bancoId: 'b1', url: 'https://s/2.jpg', thumbUrl: 'https://s/2_thumb.jpg', requiereAutenticacion: false, estado: 'PENDIENTE', subidaPor: 'ana', tamanoBytes: 2_621_440, fechaSubida: '2026-10-01', parecidas: [] },
      ]))),
      aprobarImagen: vi.fn(() => of({})),
      rechazarImagen: vi.fn(() => of(undefined)),
    };
    confirmation = { ask: vi.fn(async () => true), pedirMotivo: vi.fn(async () => 'Spam o publicidad') };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        // Los tamaños de las fotos siguen el locale de la aplicación
        { provide: LOCALE_ID, useValue: 'es-ES' },
        provideRouter([]),
        { provide: AdminApi, useValue: api },
        { provide: Confirmation, useValue: confirmation },
        { provide: Notifier, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    });
  });

  function boton(fixture: ComponentFixture<unknown>, texto: string): HTMLButtonElement {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'))
      .find((b) => b.textContent?.includes(texto))!;
  }

  it('keeps or deletes reported reviews', async () => {
    const fixture = TestBed.createComponent(ColaResenas);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Reseñas reportadas (2)');
    expect(fixture.nativeElement.textContent).toContain('(sin comentario)');

    boton(fixture, 'Mantener').click();
    await fixture.whenStable();
    expect(api['mantenerResena']).toHaveBeenCalledWith('r1');

    boton(fixture, 'Borrar').click();
    await fixture.whenStable();
    expect(confirmation.pedirMotivo).toHaveBeenCalledWith(expect.objectContaining({ titulo: 'Borrar reseña' }));
    expect(api['eliminarResena']).toHaveBeenCalledWith('r2', 'Spam o publicidad');
    expect(fixture.nativeElement.textContent).toContain('No hay reseñas reportadas.');
  });

  it('does not delete a review when the admin cancels', async () => {
    confirmation.pedirMotivo.mockResolvedValue(undefined);
    const fixture = TestBed.createComponent(ColaResenas);
    await fixture.whenStable();

    boton(fixture, 'Borrar').click();
    await fixture.whenStable();

    expect(api['eliminarResena']).not.toHaveBeenCalled();
  });

  it('flags pending photos that look like ones already uploaded', async () => {
    const fixture = TestBed.createComponent(ColaImagenes);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    const avisos = element.querySelectorAll('.parecidas');
    expect(avisos).toHaveLength(1);
    expect(avisos[0].textContent).toContain('Se parece a 2 fotos');
    expect(avisos[0].textContent).toContain('Este banco · publicada');
    expect(avisos[0].textContent).toContain('Otro banco · rechazada');
    // La rechazada no tiene fichero: se muestra un hueco en lugar de la miniatura
    expect(avisos[0].querySelectorAll('img')).toHaveLength(1);
    expect(avisos[0].querySelector('.sin-foto')).not.toBeNull();
    expect(avisos[0].querySelector('a')?.getAttribute('href')).toBe('/bancos/b1');
  });

  it('approves and rejects photos and shows readable sizes', async () => {
    const fixture = TestBed.createComponent(ColaImagenes);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('73 B');
    expect(fixture.nativeElement.textContent).toContain('2,5 MB');

    boton(fixture, 'Aprobar').click();
    await fixture.whenStable();
    boton(fixture, 'Rechazar').click();
    await fixture.whenStable();

    expect(api['aprobarImagen']).toHaveBeenCalledWith('i1');
    expect(api['rechazarImagen']).toHaveBeenCalledWith('i2', 'Spam o publicidad');
    expect(fixture.nativeElement.textContent).toContain('No hay fotos pendientes.');
  });
});
