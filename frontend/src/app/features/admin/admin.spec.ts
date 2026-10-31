import { HttpErrorResponse } from '@angular/common/http';
import { Type } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { BancoModeracion, Page, PageRequest, Usuario } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { FakeAuthService } from '../../core/auth/testing/fake-auth.service';
import { Notifier } from '../../core/ui/notifier';
import { Confirmation } from '../../shared/ui/confirm-dialog';
import { AdminDashboard } from './admin-dashboard';
import { AdminUsuarios } from './admin-usuarios';
import { Cola } from './cola';
import { ColaBancos } from './cola-bancos';

const pagina = <T>(content: T[], total = content.length): Page<T> =>
  ({ content, page: 0, size: 20, totalElements: total, totalPages: 1 });

const banco = (id: string): BancoModeracion => ({
  id, titulo: 'Banco ' + id, descripcion: null, lat: 37, lng: -6, direccion: null, ciudad: 'Sevilla',
  estado: 'PENDIENTE', autorId: 'ana', autorNombre: 'Ana', etiquetas: ['relax'], fechaSubida: '2026-10-20T10:00:00Z',
});

describe('Cola', () => {
  it('loads pages, appends more and removes resolved items', async () => {
    const fuente = vi.fn()
      .mockReturnValueOnce(of(pagina([banco('a'), banco('b')], 3)))
      .mockReturnValueOnce(of({ ...pagina([banco('c')], 3), page: 1 }));
    const cola = new Cola<BancoModeracion>(fuente, 2);

    await cola.cargar();
    expect(cola.hayMas()).toBe(true);
    await cola.cargarMas();
    expect(fuente).toHaveBeenLastCalledWith({ page: 1, size: 2 });
    expect(cola.items().map((b) => b.id)).toEqual(['a', 'b', 'c']);

    cola.quitar('b');
    expect(cola.items().map((b) => b.id)).toEqual(['a', 'c']);
    expect(cola.total()).toBe(2);
  });

  it('does not skip items when resolved ones leave the server queue', async () => {
    // Cola del servidor: lo que se resuelve desaparece y el resto se desplaza
    const servidor = ['a', 'b', 'c', 'd', 'e'].map(banco);
    const fuente = vi.fn(({ page = 0, size = 20 }: PageRequest) =>
      of(pagina(servidor.slice(page * size, page * size + size), servidor.length)));
    const cola = new Cola<BancoModeracion>(fuente, 2);
    const resolver = (id: string) => {
      servidor.splice(servidor.findIndex((b) => b.id === id), 1);
      cola.quitar(id);
    };

    await cola.cargar();
    resolver('a');
    await cola.cargarMas();
    expect(cola.items().map((b) => b.id)).toEqual(['b', 'c']);

    resolver('b');
    resolver('c');
    await vi.waitFor(() => expect(cola.items().map((b) => b.id)).toEqual(['d', 'e']));
    expect(cola.hayMas()).toBe(false);
  });

  it('keeps an error message when the API fails', async () => {
    const cola = new Cola<BancoModeracion>(() => throwError(() => new HttpErrorResponse({ status: 503 })));

    await cola.cargar();

    expect(cola.error()).toBe('El servidor no está disponible ahora mismo.');
    expect(cola.cargando()).toBe(false);
  });
});

describe('admin screens', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let auth: FakeAuthService;
  let confirmation: { ask: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    auth = new FakeAuthService();
    auth.signIn({ uid: 'root', rol: 'ADMIN' });
    api = {
      estadisticas: vi.fn(() => of({ bancosPendientes: 3, bancosPublicados: 40, bancosOcultos: 1, resenasReportadas: 2,
        imagenesPendientes: 5, totalResenas: 120, totalUsuarios: 33 })),
      bancos: vi.fn(() => of(pagina([banco('a'), banco('b')]))),
      aprobarBanco: vi.fn(() => of(banco('a'))),
      rechazarBanco: vi.fn(() => of(banco('b'))),
      usuarios: vi.fn(() => of(pagina<Usuario>([
        { id: 'root', email: 'root@x', nombre: 'Root', avatarUrl: null, ciudad: null, rol: 'ADMIN', fechaRegistro: '2026-01-01' },
        { id: 'ana', email: 'ana@x', nombre: 'Ana', avatarUrl: null, ciudad: null, rol: 'USER', fechaRegistro: '2026-02-01' },
      ]))),
      cambiarRol: vi.fn(),
    };
    confirmation = { ask: vi.fn(async () => true) };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AdminApi, useValue: api },
        { provide: AuthService, useValue: auth },
        { provide: Confirmation, useValue: confirmation },
        { provide: Notifier, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    });
  });

  async function render<T>(component: Type<T>): Promise<ComponentFixture<T>> {
    const fixture = TestBed.createComponent(component);
    await fixture.whenStable();
    return fixture;
  }

  it('dashboard links each pending count to its queue', async () => {
    const element: HTMLElement = (await render(AdminDashboard)).nativeElement;

    expect(element.querySelector('a[href="/bancos"]')?.textContent).toContain('3');
    expect(element.textContent).toContain('fotos pendientes');
    expect(element.textContent).toContain('33');
  });

  it('approving a bench removes it from the queue', async () => {
    const fixture = await render(ColaBancos);
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Bancos pendientes (2)');

    Array.from(element.querySelectorAll('button')).find((b) => b.textContent?.includes('Aprobar'))!.click();
    await fixture.whenStable();

    expect(api['aprobarBanco']).toHaveBeenCalledWith('a');
    expect(element.textContent).toContain('Bancos pendientes (1)');
    expect(element.textContent).not.toContain('Banco a');
  });

  it('admins cannot change their own role from the table', async () => {
    const element: HTMLElement = (await render(AdminUsuarios)).nativeElement;
    const selects = element.querySelectorAll('mat-select');

    expect(selects[0].getAttribute('aria-disabled')).toBe('true');
    expect(selects[1].getAttribute('aria-disabled')).toBe('false');
  });
});
