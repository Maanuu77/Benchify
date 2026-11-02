import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { Router } from '@angular/router';
import { Notificacion, TipoNotificacion } from '../api/models';
import { NotificacionesService } from '../notificaciones/notificaciones.service';

const ICONOS: Record<TipoNotificacion, string> = {
  BANCO_APROBADO: 'check_circle',
  BANCO_RECHAZADO: 'block',
  FOTO_APROBADA: 'photo_library',
  FOTO_RECHAZADA: 'hide_image',
  RESENA_RETIRADA: 'comments_disabled',
};

/** Campana de la cabecera: contador de avisos sin leer y menú con los más recientes. */
@Component({
  selector: 'bf-campana-notificaciones',
  imports: [DatePipe, MatIcon, MatMenu, MatMenuItem, MatMenuTrigger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button type="button" class="campana" [matMenuTriggerFor]="menu" (menuOpened)="cargar()"
            [attr.aria-label]="servicio.noLeidas() ? 'Notificaciones, ' + servicio.noLeidas() + ' sin leer' : 'Notificaciones'">
      <mat-icon aria-hidden="true">{{ servicio.noLeidas() ? 'notifications_active' : 'notifications' }}</mat-icon>
      @if (servicio.noLeidas(); as total) {
        <span class="contador" aria-hidden="true">{{ total > 9 ? '9+' : total }}</span>
      }
    </button>
    <mat-menu #menu="matMenu" xPosition="before" class="menu-notificaciones">
      <div class="cabecera-menu">
        <span>Notificaciones</span>
        @if (servicio.noLeidas()) {
          <button type="button" class="todas" (click)="$event.stopPropagation(); marcarTodas()">Marcar todas como leídas</button>
        }
      </div>
      @if (avisos() === null) {
        <p class="vacio">Cargando…</p>
      } @else if (avisos()!.length === 0) {
        <p class="vacio">No tienes notificaciones.</p>
      } @else {
        @for (aviso of avisos(); track aviso.id) {
          <button mat-menu-item type="button" class="aviso" [class.no-leido]="!aviso.leida" (click)="abrir(aviso)">
            <mat-icon aria-hidden="true">{{ iconos[aviso.tipo] }}</mat-icon>
            <span class="texto">
              <span class="titulo">{{ aviso.titulo }}</span>
              @if (aviso.mensaje) {
                <span class="mensaje">{{ aviso.mensaje }}</span>
              }
              <span class="fecha">{{ aviso.fecha | date: 'd MMM, HH:mm' }}</span>
            </span>
          </button>
        }
      }
    </mat-menu>
  `,
  styles: `
    .campana {
      position: relative;
      display: grid;
      place-items: center;
      width: 44px;
      height: 44px;
      border: 0;
      border-radius: 50%;
      background: none;
      color: var(--bf-ink);
      cursor: pointer;

      &:hover {
        background: var(--bf-surface-2);
      }
    }
    .contador {
      position: absolute;
      top: 4px;
      right: 2px;
      min-width: 18px;
      height: 18px;
      padding: 0 5px;
      box-sizing: border-box;
      border-radius: var(--bf-r-pill);
      background: var(--bf-danger);
      color: #fff;
      font: 700 11px/18px var(--bf-font);
      text-align: center;
    }
    .cabecera-menu {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 8px 16px;
      font: 600 15px var(--bf-font);
    }
    .todas {
      border: 0;
      background: none;
      color: var(--bf-accent-ink);
      font: 600 13px var(--bf-font);
      cursor: pointer;
    }
    .vacio {
      margin: 0;
      padding: 12px 16px 16px;
      color: var(--bf-ink-2);
    }
    .aviso {
      height: auto;
      min-height: 64px;
      padding-block: 8px;

      &.no-leido {
        background: var(--bf-accent-soft);
      }
    }
    .texto {
      display: flex;
      flex-direction: column;
      gap: 2px;
      white-space: normal;
      line-height: 1.3;
    }
    .titulo {
      font-weight: 600;
    }
    .mensaje,
    .fecha {
      font-size: 13px;
      color: var(--bf-ink-2);
    }
  `,
})
export class CampanaNotificaciones {
  protected readonly servicio = inject(NotificacionesService);
  private readonly router = inject(Router);

  protected readonly iconos = ICONOS;
  protected readonly avisos = signal<Notificacion[] | null>(null);

  protected async cargar(): Promise<void> {
    this.avisos.set(null);
    try {
      this.avisos.set(await this.servicio.recientes());
    } catch {
      this.avisos.set([]);
    }
  }

  protected async abrir(aviso: Notificacion): Promise<void> {
    if (!aviso.leida) {
      await this.servicio.marcarLeida(aviso.id).catch(() => undefined);
    }
    if (aviso.enlace) {
      await this.router.navigateByUrl(aviso.enlace);
    }
  }

  protected async marcarTodas(): Promise<void> {
    await this.servicio.marcarTodasLeidas().catch(() => undefined);
    this.avisos.update((lista) => lista?.map((a) => ({ ...a, leida: true })) ?? null);
  }
}
