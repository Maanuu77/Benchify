import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './core/auth/auth.service';
import { FakeAuthService } from './core/auth/testing/fake-auth.service';

describe('App', () => {
  let auth: FakeAuthService;

  beforeEach(() => {
    auth = new FakeAuthService();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
  });

  it('renders the shell with brand, skip link and main landmark', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('.brand')?.textContent).toContain('Benchify');
    expect(element.querySelector('a.skip-link')?.getAttribute('href')).toBe('#contenido');
    expect(element.querySelector('main#contenido')).not.toBeNull();
  });

  it('offers login to anonymous users and extra links once signed in', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Entrar');
    expect(element.textContent).not.toContain('Añadir banco');

    auth.signIn({ rol: 'ADMIN' });
    await fixture.whenStable();
    expect(element.textContent).not.toContain('Entrar');
    expect(element.textContent).toContain('Añadir banco');
    expect(element.textContent).toContain('Moderación');
    expect(element.querySelector('[aria-label="Menú de Ana"]')).not.toBeNull();
  });
});
