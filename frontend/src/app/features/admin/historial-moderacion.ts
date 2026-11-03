import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AdminApi } from '../../core/api/admin-api';
import { AccionModeracion, EventoModeracion } from '../../core/api/models';
import { textos, textosActuales } from '../../core/i18n/idioma';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { COLA } from './admin.i18n';
import { Cola } from './cola';
import { HISTORIAL_MODERACION } from './historial-moderacion.i18n';

const ICONOS: Record<AccionModeracion, string> = {
  BANCO_APROBADO: 'check_circle',
  BANCO_RECHAZADO: 'block',
  RESENA_ELIMINADA: 'comments_disabled',
  RESENA_MANTENIDA: 'rate_review',
  IMAGEN_APROBADA: 'photo_library',
  IMAGEN_RECHAZADA: 'hide_image',
  EDICION_APROBADA: 'published_with_changes',
  EDICION_RECHAZADA: 'edit_off',
};

function acciones(nombres: Record<AccionModeracion, string>): Record<AccionModeracion, { texto: string; icono: string }> {
  const claves = Object.keys(ICONOS) as AccionModeracion[];
  return Object.fromEntries(claves.map((accion) => [accion, { texto: nombres[accion], icono: ICONOS[accion] }])) as
    Record<AccionModeracion, { texto: string; icono: string }>;
}

/** Texto e icono de cada decisión, en el idioma de la visita. */
export const ACCIONES = acciones(textosActuales(HISTORIAL_MODERACION).acciones);

/** Historial de decisiones de moderación: quién hizo qué, cuándo y por qué. */
@Component({
  selector: 'bf-historial-moderacion',
  imports: [DatePipe, RouterLink, MatButton, MatIcon, LoadingState, ErrorState, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>{{ t.titulo }} ({{ cola.total() }})</h2>
    @if (cola.error(); as mensaje) {
      <bf-error-state [message]="mensaje" (retry)="cola.cargar()" />
    } @else if (!cola.cargando() && cola.items().length === 0) {
      <bf-empty-state icon="history" [message]="t.vacio" />
    } @else {
      <ol class="historial">
        @for (evento of cola.items(); track evento.id) {
          <li>
            <mat-icon aria-hidden="true">{{ acciones[evento.accion].icono }}</mat-icon>
            <div>
              <p class="que">
                {{ acciones[evento.accion].texto }}
                @if (evento.recursoTipo === 'BANCO') {
                  · <a [routerLink]="['/bancos', evento.recursoId]">{{ tc.verBanco }}</a>
                }
              </p>
              @if (evento.motivo) {
                <p class="motivo">{{ t.motivo(evento.motivo) }}</p>
              }
              <p class="meta">
                <time [attr.datetime]="evento.fecha">{{ evento.fecha | date: 'medium' }}</time>
                · {{ t.por }} <a [routerLink]="['/usuarios', evento.moderadorId]">{{ t.unModerador }}</a>
              </p>
            </div>
          </li>
        }
      </ol>
      @if (cola.cargando()) {
        <bf-loading-state />
      } @else if (cola.hayMas()) {
        <button mat-stroked-button type="button" (click)="cola.cargarMas()">{{ tc.cargarMas }}</button>
      }
    }
  `,
  styles: `
    h2 {
      margin: 16px 0;
      font: 600 18px var(--bf-font);
    }
    .historial {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin: 0 0 16px;
      padding: 0;
      list-style: none;
    }
    li {
      display: flex;
      gap: 12px;
      padding: 14px 16px;
      border: 1px solid var(--bf-line);
      border-radius: var(--bf-r);
      background: var(--bf-surface);

      mat-icon {
        flex: none;
        color: var(--bf-accent-ink);
      }
      p {
        margin: 0;
      }
    }
    .que {
      font-weight: 600;
    }
    .motivo {
      margin-top: 4px;
      color: var(--bf-ink);
    }
    .meta {
      margin-top: 4px;
      font-size: 13px;
      color: var(--bf-ink-2);
    }
  `,
})
export class HistorialModeracion implements OnInit {
  private readonly api = inject(AdminApi);

  protected readonly t = textos(HISTORIAL_MODERACION);
  protected readonly tc = textos(COLA);
  protected readonly acciones = acciones(this.t.acciones);
  protected readonly cola = new Cola<EventoModeracion>((page) => this.api.eventos(page), 30);

  ngOnInit(): void {
    void this.cola.cargar();
  }
}
