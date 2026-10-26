import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { mensajeDeErrorAuth } from '../../core/auth/auth-errors';
import { AuthService } from '../../core/auth/auth.service';

export const passwordsMatch: ValidatorFn = (group: AbstractControl): ValidationErrors | null =>
  group.get('password')?.value === group.get('confirmacion')?.value ? null : { passwordsMismatch: true };

@Component({
  selector: 'bf-registro-page',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatCard, MatCardHeader, MatCardTitle, MatCardContent,
    MatFormField, MatLabel, MatInput, MatError, MatHint, MatButton,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './registro-page.html',
  styleUrl: './auth-page.scss',
})
export class RegistroPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly returnUrl = input<string>();

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmacion: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  protected async register(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nombre, email, password } = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set(null);
    try {
      await this.auth.register(nombre.trim(), email, password);
      const url = this.returnUrl();
      await this.router.navigateByUrl(url && url.startsWith('/') && !url.startsWith('//') ? url : '/mapa');
    } catch (error) {
      this.error.set(mensajeDeErrorAuth(error));
    } finally {
      this.submitting.set(false);
    }
  }
}
