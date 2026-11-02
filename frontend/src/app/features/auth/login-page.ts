import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatError, MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { mensajeDeErrorAuth } from '../../core/auth/auth-errors';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'bf-login-page',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatFormField, MatLabel, MatInput, MatError, MatSuffix, MatButton, MatIconButton, MatIcon,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login-page.html',
  styleUrl: './auth-page.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Página a la que volver tras iniciar sesión (?returnUrl=…). */
  readonly returnUrl = input<string>();

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verPassword = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected async loginWithEmail(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, password } = this.form.getRawValue();
    await this.run(() => this.auth.loginWithEmail(email, password));
  }

  protected async loginWithGoogle(): Promise<void> {
    await this.run(() => this.auth.loginWithGoogle());
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.submitting.set(true);
    this.error.set(null);
    try {
      await action();
      await this.router.navigateByUrl(this.safeReturnUrl());
    } catch (error) {
      this.error.set(mensajeDeErrorAuth(error));
    } finally {
      this.submitting.set(false);
    }
  }

  /** Solo rutas internas: evita redirecciones abiertas a otros dominios. */
  private safeReturnUrl(): string {
    const url = this.returnUrl();
    return url && url.startsWith('/') && !url.startsWith('//') ? url : '/mapa';
  }
}
