import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatOption, MatSelect, MatSelectChange } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { Rol, Usuario } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { textos } from '../../core/i18n/idioma';
import { Notifier } from '../../core/ui/notifier';
import { Confirmation } from '../../shared/ui/confirm-dialog';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { COLA } from './admin.i18n';
import { ADMIN_USUARIOS } from './admin-usuarios.i18n';
import { Cola } from './cola';

@Component({
  selector: 'bf-admin-usuarios',
  imports: [DatePipe, MatSelect, MatOption, MatButton, LoadingState, ErrorState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>{{ t.titulo }} ({{ cola.total() }})</h2>
    @if (cola.error(); as mensaje) {
      <bf-error-state [message]="mensaje" (retry)="cola.cargar()" />
    } @else {
      <table class="tabla">
        <caption class="sr-only">{{ t.leyenda }}</caption>
        <thead>
          <tr><th scope="col">{{ t.nombre }}</th><th scope="col">{{ t.email }}</th><th scope="col">{{ t.alta }}</th><th scope="col">{{ t.rol }}</th></tr>
        </thead>
        <tbody>
          @for (usuario of cola.items(); track usuario.id) {
            <tr>
              <td>{{ usuario.nombre }}</td>
              <td>{{ usuario.email }}</td>
              <td>{{ usuario.fechaRegistro | date: 'shortDate' }}</td>
              <td>
                <mat-select [value]="usuario.rol" [disabled]="usuario.id === miUid() || ocupado() === usuario.id"
                            [attr.aria-label]="t.rolDe(usuario.nombre)" (selectionChange)="cambiarRol(usuario, $event)">
                  <mat-option value="USER">{{ t.usuario }}</mat-option>
                  <mat-option value="ADMIN">{{ t.administrador }}</mat-option>
                </mat-select>
              </td>
            </tr>
          }
        </tbody>
      </table>
      @if (cola.cargando()) {
        <bf-loading-state />
      } @else if (cola.hayMas()) {
        <button mat-stroked-button type="button" (click)="cola.cargarMas()">{{ tc.cargarMas }}</button>
      }
      <p class="nota">{{ t.nota }}</p>
    }
  `,
  styles: `
    h2 {
      font: var(--mat-sys-title-medium);
    }
    .tabla {
      width: 100%;
      border-collapse: collapse;
    }
    th,
    td {
      padding: 8px;
      text-align: left;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    th {
      font: var(--mat-sys-label-large);
    }
    mat-select {
      max-width: 180px;
    }
    .nota {
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-small);
    }
  `,
})
export class AdminUsuarios implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly notifier = inject(Notifier);
  private readonly confirmation = inject(Confirmation);
  private readonly auth = inject(AuthService);
  protected readonly t = textos(ADMIN_USUARIOS);
  protected readonly tc = textos(COLA);

  protected readonly cola = new Cola<Usuario>((page) => this.api.usuarios(page), 50);
  protected readonly ocupado = signal<string | null>(null);
  protected readonly miUid = () => this.auth.session()?.uid;

  ngOnInit(): void {
    void this.cola.cargar();
  }

  protected async cambiarRol(usuario: Usuario, event: MatSelectChange): Promise<void> {
    const rol = event.value as Rol;
    const ok = await this.confirmation.ask({
      titulo: this.t.cambiarRol,
      mensaje: rol === 'ADMIN' ? this.t.seraAdmin(usuario.nombre) : this.t.dejaraDeSerAdmin(usuario.nombre),
      confirmar: this.t.cambiar,
    });
    if (!ok) {
      event.source.value = usuario.rol;
      return;
    }
    this.ocupado.set(usuario.id);
    try {
      this.cola.reemplazar(await firstValueFrom(this.api.cambiarRol(usuario.id, rol)));
      this.notifier.success(this.t.rolActualizado(usuario.nombre));
    } catch (error) {
      event.source.value = usuario.rol;
      this.notifier.error(error);
    } finally {
      this.ocupado.set(null);
    }
  }
}
