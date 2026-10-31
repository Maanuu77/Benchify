import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { FakeAuthService } from '../../core/auth/testing/fake-auth.service';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let auth: FakeAuthService;
  let router: Router;
  let element: HTMLElement;

  beforeEach(async () => {
    auth = new FakeAuthService();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(LoginPage);
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  function type(selector: string, value: string): void {
    const input = element.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function submit(): Promise<void> {
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    return fixture.whenStable();
  }

  it('does not call Firebase while the form is invalid', async () => {
    type('input[type=email]', 'no-es-un-email');
    await submit();

    expect(auth.loginWithEmail).not.toHaveBeenCalled();
    expect(element.textContent).toContain('Introduce un email válido');
  });

  it('signs in and returns to the requested internal page', async () => {
    fixture.componentRef.setInput('returnUrl', '/bancos/nuevo');
    type('input[type=email]', 'ana@benchify.dev');
    type('input[type=password]', 'secreto');
    await submit();

    expect(auth.loginWithEmail).toHaveBeenCalledWith('ana@benchify.dev', 'secreto');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/bancos/nuevo');
  });

  it('ignores external return URLs to avoid open redirects', async () => {
    fixture.componentRef.setInput('returnUrl', '//evil.example');
    await (fixture.componentInstance as unknown as { loginWithGoogle(): Promise<void> }).loginWithGoogle();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/mapa');
  });

  it('shows a translated error when Firebase rejects the credentials', async () => {
    auth.loginWithEmail.mockRejectedValueOnce({ code: 'auth/invalid-credential' });
    type('input[type=email]', 'ana@benchify.dev');
    type('input[type=password]', 'mala');
    await submit();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Email o contraseña incorrectos');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('shows and hides the password', async () => {
    const password = element.querySelector<HTMLInputElement>('[formcontrolname=password]')!;
    const boton = element.querySelector<HTMLButtonElement>('button[aria-label="Mostrar contraseña"]')!;
    expect(password.type).toBe('password');

    boton.click();
    await fixture.whenStable();

    expect(password.type).toBe('text');
    expect(boton.getAttribute('aria-label')).toBe('Ocultar contraseña');
  });
});
