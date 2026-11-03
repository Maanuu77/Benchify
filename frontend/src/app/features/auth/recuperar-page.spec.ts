import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { FakeAuthService } from '../../core/auth/testing/fake-auth.service';
import { RecuperarPage } from './recuperar-page';

describe('RecuperarPage', () => {
  let fixture: ComponentFixture<RecuperarPage>;
  let element: HTMLElement;
  let auth: FakeAuthService;

  beforeEach(async () => {
    auth = new FakeAuthService();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
    fixture = TestBed.createComponent(RecuperarPage);
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  async function enviar(email: string): Promise<void> {
    const input = element.querySelector<HTMLInputElement>('[formcontrolname=email]')!;
    input.value = email;
    input.dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('sends the reset link and confirms without revealing whether the account exists', async () => {
    await enviar('  ana@benchify.dev ');

    expect(auth.recuperarPassword).toHaveBeenCalledWith('ana@benchify.dev');
    expect(element.querySelector('[role=status]')?.textContent).toContain('Si hay una cuenta con ese email');
  });

  it('answers the same for unknown accounts', async () => {
    auth.recuperarPassword.mockRejectedValueOnce({ code: 'auth/user-not-found' });

    await enviar('nadie@benchify.dev');

    expect(element.querySelector('[role=status]')).not.toBeNull();
  });

  it('does not call Firebase with an invalid email', async () => {
    await enviar('no-es-un-email');

    expect(auth.recuperarPassword).not.toHaveBeenCalled();
  });
});
