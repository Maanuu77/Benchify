import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Page, Resena } from '../../../core/api/models';
import { ResenasApi } from '../../../core/api/resenas-api';
import { AuthService } from '../../../core/auth/auth.service';
import { FakeAuthService } from '../../../core/auth/testing/fake-auth.service';
import { Notifier } from '../../../core/ui/notifier';
import { Confirmation } from '../../../shared/ui/confirm-dialog';
import { ResenasSection } from './resenas-section';

const resena = (id: string, autorId: string): Resena => ({
  id, bancoId: 'b1', autorId, autorNombre: 'Autor ' + autorId, puntuacion: 4, comentario: 'Bien',
  fecha: '2026-10-01T10:00:00Z', fechaEdicion: null,
});
const pagina = (content: Resena[], total = content.length): Page<Resena> =>
  ({ content, page: 0, size: 20, totalElements: total, totalPages: 1 });

describe('ResenasSection', () => {
  let fixture: ComponentFixture<ResenasSection>;
  let element: HTMLElement;
  let auth: FakeAuthService;
  let api: Record<'deBanco' | 'mia' | 'publicar' | 'editar' | 'eliminar' | 'reportar', ReturnType<typeof vi.fn>>;
  let confirmation: { ask: ReturnType<typeof vi.fn> };

  async function crear(resenas: Resena[], autorBancoId = 'autor-banco'): Promise<void> {
    api.deBanco.mockReturnValue(of(pagina(resenas)));
    fixture = TestBed.createComponent(ResenasSection);
    fixture.componentRef.setInput('bancoId', 'b1');
    fixture.componentRef.setInput('autorBancoId', autorBancoId);
    element = fixture.nativeElement;
    await fixture.whenStable();
  }

  beforeEach(() => {
    auth = new FakeAuthService();
    api = {
      deBanco: vi.fn(), mia: vi.fn(() => of(null)), publicar: vi.fn(() => of(resena('nueva', 'ana'))), editar: vi.fn(() => of({})),
      eliminar: vi.fn(() => of(undefined)), reportar: vi.fn(() => of(undefined)),
    };
    confirmation = { ask: vi.fn(async () => true) };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: ResenasApi, useValue: api },
        { provide: Confirmation, useValue: confirmation },
        { provide: Notifier, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    });
  });

  it('invites anonymous users to sign in instead of showing the form', async () => {
    await crear([resena('r1', 'luis')]);

    expect(element.querySelector('form')).toBeNull();
    expect(element.textContent).toContain('Inicia sesión para dejar tu reseña');
    expect(element.textContent).toContain('Autor luis');
  });

  it('hides the form from the bench author and from users who already reviewed', async () => {
    auth.signIn({ uid: 'autor-banco' });
    await crear([]);
    expect(element.querySelector('form')).toBeNull();

    auth.signIn({ uid: 'ana' });
    api.mia.mockReturnValue(of(resena('r1', 'ana')));
    await crear([resena('r1', 'ana')]);
    expect(element.querySelector('form')).toBeNull();
    expect(element.textContent).toContain('Editar');
  });

  it('hides the form when the own review is beyond the loaded page', async () => {
    auth.signIn({ uid: 'ana' });
    api.mia.mockReturnValue(of(resena('antigua', 'ana')));
    api.deBanco.mockReturnValue(of(pagina([resena('r1', 'luis')], 25)));
    fixture = TestBed.createComponent(ResenasSection);
    fixture.componentRef.setInput('bancoId', 'b1');
    fixture.componentRef.setInput('autorBancoId', 'x');
    await fixture.whenStable();

    expect(api.mia).toHaveBeenCalledWith('b1');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('publishes a review, reloads the list and notifies the page', async () => {
    auth.signIn({ uid: 'ana' });
    await crear([]);
    const emitido = vi.fn();
    fixture.componentInstance.valoracionCambiada.subscribe(emitido);

    element.querySelectorAll<HTMLButtonElement>('button[role=radio]')[4].click();
    const textarea = element.querySelector('textarea')!;
    textarea.value = '  Genial  ';
    textarea.dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(api.publicar).toHaveBeenCalledWith('b1', { puntuacion: 5, comentario: 'Genial' });
    expect(api.deBanco).toHaveBeenCalledTimes(2);
    expect(emitido).toHaveBeenCalled();
  });

  it('reports someone else\'s review after confirmation', async () => {
    auth.signIn({ uid: 'ana' });
    await crear([resena('r1', 'luis')]);

    element.querySelector<HTMLButtonElement>('button[aria-label^="Reportar"]')!.click();
    await fixture.whenStable();

    expect(confirmation.ask).toHaveBeenCalled();
    expect(api.reportar).toHaveBeenCalledWith('r1');
  });

  it('offers to load more when there are more pages', async () => {
    api.deBanco.mockReturnValue(of(pagina([resena('r1', 'luis')], 25)));
    fixture = TestBed.createComponent(ResenasSection);
    fixture.componentRef.setInput('bancoId', 'b1');
    fixture.componentRef.setInput('autorBancoId', 'x');
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Ver más reseñas');
  });
});
