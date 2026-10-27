import { FormControl, FormGroup } from '@angular/forms';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { FakeAuthService } from '../../core/auth/testing/fake-auth.service';
import { passwordsMatch, RegistroPage } from './registro-page';

describe('RegistroPage', () => {
  let fixture: ComponentFixture<RegistroPage>;
  let auth: FakeAuthService;
  let element: HTMLElement;

  beforeEach(async () => {
    auth = new FakeAuthService();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(RegistroPage);
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  function fill(nombre: string, email: string, password: string, confirmacion: string): Promise<void> {
    const values: Record<string, string> = { nombre, email, password, confirmacion };
    for (const [name, value] of Object.entries(values)) {
      const input = element.querySelector<HTMLInputElement>(`input[formcontrolname=${name}]`)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
      input.dispatchEvent(new Event('blur'));
    }
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    return fixture.whenStable();
  }

  it('validates that both passwords match', () => {
    const group = new FormGroup({ password: new FormControl('abcdefgh'), confirmacion: new FormControl('abcdefgx') });
    expect(passwordsMatch(group)).toEqual({ passwordsMismatch: true });
    group.controls.confirmacion.setValue('abcdefgh');
    expect(passwordsMatch(group)).toBeNull();
  });

  it('registers with a trimmed display name', async () => {
    await fill('  Ana García ', 'ana@benchify.dev', 'muy-secreta', 'muy-secreta');

    expect(auth.register).toHaveBeenCalledWith('Ana García', 'ana@benchify.dev', 'muy-secreta');
  });

  it('blocks short or mismatching passwords', async () => {
    await fill('Ana', 'ana@benchify.dev', 'corta', 'otra');

    expect(auth.register).not.toHaveBeenCalled();
    expect(element.textContent).toContain('Mínimo 8 caracteres');
    expect(element.textContent).toContain('Las contraseñas no coinciden');
  });
});
