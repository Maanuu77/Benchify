import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { IncidenciasApi } from '../../../core/api/incidencias-api';
import { AuthService } from '../../../core/auth/auth.service';
import { FakeAuthService } from '../../../core/auth/testing/fake-auth.service';
import { Notifier } from '../../../core/ui/notifier';
import { AvisoIncidencia, AvisosBanco, IncidenciaDialog } from './avisos-banco';

describe('AvisosBanco', () => {
  let fixture: ComponentFixture<AvisosBanco>;
  let element: HTMLElement;
  let auth: FakeAuthService;
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let resultado: AvisoIncidencia | undefined;
  let dialog: { open: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    auth = new FakeAuthService();
    api = {
      resumen: vi.fn(() => of([{ tipo: 'SUCIO', total: 2 }])),
      reportar: vi.fn(() => of({})),
    };
    resultado = { tipo: 'ROTO', comentario: 'Tabla suelta' };
    dialog = { open: vi.fn(() => ({ afterClosed: () => of(resultado) })) };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: IncidenciasApi, useValue: api },
        { provide: MatDialog, useValue: dialog },
        { provide: Notifier, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(AvisosBanco);
    fixture.componentRef.setInput('bancoId', 'b1');
    fixture.componentRef.setInput('tituloBanco', 'Glorieta');
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  const avisar = () => element.querySelector<HTMLButtonElement>('.avisar')!;

  it('shows the open reports summary', () => {
    expect(element.textContent).toContain('La comunidad avisa');
    expect(element.textContent).toContain('está sucio (2)');
  });

  it('sends a report and refreshes the summary', async () => {
    auth.signIn();
    avisar().click();
    await fixture.whenStable();

    expect(dialog.open).toHaveBeenCalledWith(IncidenciaDialog, expect.objectContaining({ data: 'Glorieta' }));
    expect(api['reportar']).toHaveBeenCalledWith('b1', 'ROTO', 'Tabla suelta');
    expect(api['resumen']).toHaveBeenCalledTimes(2);
  });

  it('does nothing when the dialog is cancelled', async () => {
    auth.signIn();
    resultado = undefined;
    avisar().click();
    await fixture.whenStable();

    expect(api['reportar']).not.toHaveBeenCalled();
  });

  it('sends anonymous users to log in', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    avisar().click();
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/bancos/b1' } });
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('hides the button for unpublished benches', async () => {
    fixture.componentRef.setInput('publicado', false);
    await fixture.whenStable();
    expect(avisar()).toBeNull();
  });
});
