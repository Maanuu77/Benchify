import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AdminApi } from '../../core/api/admin-api';
import { Loadable, loadable } from '../../core/api/api-error';
import { EstadisticasModeracion } from '../../core/api/models';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';

@Component({
  selector: 'bf-admin-dashboard',
  imports: [RouterLink, MatIcon, LoadingState, ErrorState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @switch (estado().status) {
      @case ('loading') { <bf-loading-state message="Cargando resumen…" /> }
      @case ('error') { <bf-error-state message="No hemos podido cargar el resumen." [retryable]="false" /> }
      @case ('success') {
        @let e = estadisticas()!;
        <h2 class="sr-only">Pendiente de revisar</h2>
        <div class="tarjetas">
          <a class="tarjeta pendiente" routerLink="bancos">
            <mat-icon aria-hidden="true">add_location_alt</mat-icon>
            <strong>{{ e.bancosPendientes }}</strong><span>bancos pendientes</span>
          </a>
          <a class="tarjeta pendiente" routerLink="resenas">
            <mat-icon aria-hidden="true">flag</mat-icon>
            <strong>{{ e.resenasReportadas }}</strong><span>reseñas reportadas</span>
          </a>
          <a class="tarjeta pendiente" routerLink="imagenes">
            <mat-icon aria-hidden="true">photo_library</mat-icon>
            <strong>{{ e.imagenesPendientes }}</strong><span>fotos pendientes</span>
          </a>
        </div>
        <h2>Contenido</h2>
        <dl class="totales">
          <div><dt>Bancos publicados</dt><dd>{{ e.bancosPublicados }}</dd></div>
          <div><dt>Bancos ocultos</dt><dd>{{ e.bancosOcultos }}</dd></div>
          <div><dt>Reseñas</dt><dd>{{ e.totalResenas }}</dd></div>
          <div><dt>Usuarios</dt><dd>{{ e.totalUsuarios }}</dd></div>
        </dl>
      }
    }
  `,
  styles: `
    .tarjetas,
    .totales {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 12px;
      margin: 0 0 24px;
    }
    .tarjeta {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 16px;
      border-radius: 12px;
      text-decoration: none;
      color: var(--mat-sys-on-tertiary-container);
      background: var(--mat-sys-tertiary-container);
    }
    .tarjeta strong {
      font: var(--mat-sys-display-small);
    }
    .totales div {
      padding: 12px 16px;
      border-radius: 12px;
      background: var(--mat-sys-surface-container);
    }
    dt {
      color: var(--mat-sys-on-surface-variant);
    }
    dd {
      margin: 4px 0 0;
      font: var(--mat-sys-headline-small);
    }
    h2 {
      font: var(--mat-sys-title-medium);
    }
    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
    }
  `,
})
export class AdminDashboard {
  protected readonly estado = toSignal(loadable(inject(AdminApi).estadisticas()),
    { initialValue: { status: 'loading' } as Loadable<EstadisticasModeracion> });

  protected estadisticas(): EstadisticasModeracion | null {
    const estado = this.estado();
    return estado.status === 'success' ? estado.value : null;
  }
}
