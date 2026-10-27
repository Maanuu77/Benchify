import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { BancosApi } from '../../core/api/bancos-api';
import { Page } from '../../core/api/models';
import { ResenasApi } from '../../core/api/resenas-api';
import { UsuariosApi } from '../../core/api/usuarios-api';
import { AuthService } from '../../core/auth/auth.service';
import { FakeAuthService } from '../../core/auth/testing/fake-auth.service';
import { Notifier } from '../../core/ui/notifier';
import { PerfilPage } from './perfil-page';

const vacia = <T>(): Page<T> => ({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });

describe('PerfilPage', () => {
  let fixture: ComponentFixture<PerfilPage>;
  let element: HTMLElement;
  let auth: FakeAuthService;
  let usuarios: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    auth = new FakeAuthService();
    usuarios = {
      perfil: vi.fn((id: string) => of({ id, nombre: id === 'ana' ? 'Ana' : 'Luis', avatarUrl: null, ciudad: 'Sevilla', fechaRegistro: '2026-01-10T00:00:00Z' })),
      estadisticas: vi.fn(() => of({ bancosPublicados: 2, resenas: 5, favoritos: 3, checkIns: 7 })),
      misFavoritos: vi.fn(() => of(vacia())),
      misCheckIns: vi.fn(() => of(vacia())),
      actualizarPerfil: vi.fn(() => of({})),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: UsuariosApi, useValue: usuarios },
        { provide: BancosApi, useValue: { deUsuario: vi.fn(() => of(vacia())) } },
        { provide: ResenasApi, useValue: { deUsuario: vi.fn(() => of(vacia())) } },
        { provide: Notifier, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    });
  });

  async function crear(id?: string): Promise<void> {
    fixture = TestBed.createComponent(PerfilPage);
    if (id) fixture.componentRef.setInput('id', id);
    element = fixture.nativeElement;
    await fixture.whenStable();
  }

  it('shows the signed-in user profile with private tabs', async () => {
    auth.signIn({ uid: 'ana' });
    await crear();

    expect(usuarios['perfil']).toHaveBeenCalledWith('ana');
    expect(element.querySelector('h1')?.textContent).toContain('Ana');
    expect(element.textContent).toContain('7');
    expect(element.textContent).toContain('Favoritos');
    expect(element.textContent).toContain('Editar perfil');
    expect(usuarios['misFavoritos']).toHaveBeenCalled();
  });

  it('shows public profiles without private tabs or editing', async () => {
    auth.signIn({ uid: 'ana' });
    await crear('luis');

    expect(element.querySelector('h1')?.textContent).toContain('Luis');
    expect(element.textContent).not.toContain('Editar perfil');
    expect(Array.from(element.querySelectorAll('[role=tab]')).map((t) => t.textContent?.trim()))
      .toEqual(['Bancos subidos', 'Reseñas']);
    expect(usuarios['misFavoritos']).not.toHaveBeenCalled();
  });

  it('updates the profile name and city', async () => {
    auth.signIn({ uid: 'ana' });
    await crear();

    Array.from(element.querySelectorAll('button')).find((b) => b.textContent?.includes('Editar perfil'))!.click();
    await fixture.whenStable();
    const [nombre, ciudad] = Array.from(element.querySelectorAll<HTMLInputElement>('form input'));
    nombre.value = ' Ana María ';
    nombre.dispatchEvent(new Event('input'));
    ciudad.value = 'Triana';
    ciudad.dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(usuarios['actualizarPerfil']).toHaveBeenCalledWith('Ana María', 'Triana');
  });
});
