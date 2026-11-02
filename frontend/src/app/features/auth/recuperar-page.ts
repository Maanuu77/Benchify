import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { mensajeDeErrorAuth } from '../../core/auth/auth-errors';
import { AuthService } from '../../core/auth/auth.service';

/**
 * Envía el enlace de Firebase para elegir una contraseña nueva. La respuesta es la misma exista o no
 * la cuenta, para no revelar qué emails están registrados.
 */
@Component({
  selector: 'bf-recuperar-page',
  imports: [ReactiveFormsModule, RouterLink, MatFormField, MatLabel, MatInput, MatError, MatButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page">
      <aside class="marca-panel" aria-hidden="true">
        <img src="logo-mark.svg" alt="" width="72" height="72" />
        <p class="lema">Encuentra tu banco.<br />Siéntate. Compártelo.</p>
        <p class="sub">El mapa colaborativo de los bancos públicos de tu ciudad.</p>
      </aside>

      <div class="form-panel">
        <div class="card">
          <img class="logo-movil" src="logo-mark.svg" alt="" width="44" height="44" />
          <h1>Recuperar la contraseña</h1>
          @if (enviado()) {
            <p class="intro" role="status">
              Si hay una cuenta con ese email, te hemos enviado un enlace para elegir una contraseña nueva.
              Revisa también la carpeta de correo no deseado.
            </p>
            <a mat-flat-button routerLink="/login">Volver a iniciar sesión</a>
          } @else {
            <p class="intro">Escribe el email de tu cuenta y te enviaremos un enlace para cambiarla.</p>
            <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
              <mat-form-field appearance="outline">
                <mat-label>Email</mat-label>
                <input matInput type="email" formControlName="email" autocomplete="email" required />
                @if (form.controls.email.hasError('email')) {
                  <mat-error>Introduce un email válido</mat-error>
                } @else if (form.controls.email.hasError('required')) {
                  <mat-error>El email es obligatorio</mat-error>
                }
              </mat-form-field>
              @if (error(); as mensaje) {
                <p class="error" role="alert">{{ mensaje }}</p>
              }
              <button mat-flat-button type="submit" [disabled]="enviando()">Enviar enlace</button>
            </form>
            <p class="switch"><a routerLink="/login">Volver a iniciar sesión</a></p>
          }
        </div>
      </div>
    </section>
  `,
  styleUrl: './auth-page.scss',
})
export class RecuperarPage {
  private readonly auth = inject(AuthService);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
  });
  protected readonly enviando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly error = signal<string | null>(null);

  protected async enviar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    try {
      await this.auth.recuperarPassword(this.form.getRawValue().email.trim());
      this.enviado.set(true);
    } catch (error) {
      // Una cuenta que no existe se trata como enviada: no se revela qué emails están registrados
      if ((error as { code?: string }).code === 'auth/user-not-found') {
        this.enviado.set(true);
      } else {
        this.error.set(mensajeDeErrorAuth(error));
      }
    } finally {
      this.enviando.set(false);
    }
  }
}
