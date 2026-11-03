import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { Loadable, loadable } from '../../core/api/api-error';
import { EstadisticasModeracion } from '../../core/api/models';
import { textos } from '../../core/i18n/idioma';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { ADMIN_DASHBOARD } from './admin-dashboard.i18n';

@Component({
  selector: 'bf-admin-dashboard',
  imports: [RouterLink, MatIcon, LoadingState, ErrorState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @switch (estado().status) {
      @case ('loading') { <bf-loading-state [message]="t.cargando" /> }
      @case ('error') { <bf-error-state [message]="t.error" (retry)="recargar()" /> }
      @case ('success') {
        @let e = estadisticas()!;
        <h2 class="sr-only">{{ t.pendiente }}</h2>
        <div class="tarjetas">
          <a class="tarjeta pendiente" routerLink="bancos">
            <mat-icon aria-hidden="true">add_location_alt</mat-icon>
            <strong>{{ e.bancosPendientes }}</strong><span>{{ t.bancosPendientes(e.bancosPendientes) }}</span>
          </a>
          <a class="tarjeta pendiente" routerLink="ediciones">
            <mat-icon aria-hidden="true">published_with_changes</mat-icon>
            <strong>{{ e.edicionesPendientes }}</strong><span>{{ t.edicionesPendientes(e.edicionesPendientes) }}</span>
          </a>
          <a class="tarjeta pendiente" routerLink="resenas">
            <mat-icon aria-hidden="true">flag</mat-icon>
            <strong>{{ e.resenasReportadas }}</strong><span>{{ t.resenasReportadas(e.resenasReportadas) }}</span>
          </a>
          <a class="tarjeta pendiente" routerLink="imagenes">
            <mat-icon aria-hidden="true">photo_library</mat-icon>
            <strong>{{ e.imagenesPendientes }}</strong><span>{{ t.fotosPendientes(e.imagenesPendientes) }}</span>
          </a>
        </div>
        <h2>{{ t.contenido }}</h2>
        <dl class="totales">
          <div><dt>{{ t.bancosPublicados }}</dt><dd>{{ e.bancosPublicados }}</dd></div>
          <div><dt>{{ t.bancosOcultos }}</dt><dd>{{ e.bancosOcultos }}</dd></div>
          <div><dt>{{ t.resenas }}</dt><dd>{{ e.totalResenas }}</dd></div>
          <div><dt>{{ t.usuarios }}</dt><dd>{{ e.totalUsuarios }}</dd></div>
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
      border-radius: var(--bf-r);
      text-decoration: none;
      color: var(--bf-ink);
      background: var(--bf-accent-soft);
      transition: transform 120ms var(--bf-ease), box-shadow var(--bf-dur) var(--bf-ease);

      &:hover {
        transform: translateY(-2px);
        box-shadow: var(--bf-sh-2);
      }
      mat-icon {
        color: var(--bf-accent-ink);
      }
    }
    .tarjeta strong {
      font: 700 40px/1 var(--bf-font);
      letter-spacing: -0.03em;
    }
    .tarjeta span {
      color: var(--bf-ink-2);
    }
    .totales div {
      padding: 14px 16px;
      border-radius: var(--bf-r);
      background: var(--bf-surface-2);
    }
    dt {
      font-size: 13px;
      color: var(--bf-ink-2);
    }
    dd {
      margin: 4px 0 0;
      font: 700 24px var(--bf-font);
    }
    h2 {
      font: 600 17px var(--bf-font);
    }
  `,
})
export class AdminDashboard {
  private readonly api = inject(AdminApi);
  private readonly recarga = signal(0);
  protected readonly t = textos(ADMIN_DASHBOARD);

  protected readonly estado = toSignal(
    toObservable(this.recarga).pipe(switchMap(() => loadable(this.api.estadisticas()))),
    { initialValue: { status: 'loading' } as Loadable<EstadisticasModeracion> },
  );

  protected recargar(): void {
    this.recarga.update((n) => n + 1);
  }

  protected estadisticas(): EstadisticasModeracion | null {
    const estado = this.estado();
    return estado.status === 'success' ? estado.value : null;
  }
}
