import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AdminApi } from '../../core/api/admin-api';
import { AccionModeracion, EventoModeracion } from '../../core/api/models';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { Cola } from './cola';

export const ACCIONES: Record<AccionModeracion, { texto: string; icono: string }> = {
  BANCO_APROBADO: { texto: 'Banco publicado', icono: 'check_circle' },
  BANCO_RECHAZADO: { texto: 'Banco rechazado', icono: 'block' },
  RESENA_ELIMINADA: { texto: 'Reseña borrada', icono: 'comments_disabled' },
  RESENA_MANTENIDA: { texto: 'Reseña mantenida', icono: 'rate_review' },
  IMAGEN_APROBADA: { texto: 'Foto aprobada', icono: 'photo_library' },
  IMAGEN_RECHAZADA: { texto: 'Foto rechazada', icono: 'hide_image' },
  EDICION_APROBADA: { texto: 'Edición aplicada', icono: 'published_with_changes' },
  EDICION_RECHAZADA: { texto: 'Edición descartada', icono: 'edit_off' },
};

/** Historial de decisiones de moderación: quién hizo qué, cuándo y por qué. */
@Component({
  selector: 'bf-historial-moderacion',
  imports: [DatePipe, RouterLink, MatButton, MatIcon, LoadingState, ErrorState, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>Historial ({{ cola.total() }})</h2>
    @if (cola.error(); as mensaje) {
      <bf-error-state [message]="mensaje" (retry)="cola.cargar()" />
    } @else if (!cola.cargando() && cola.items().length === 0) {
      <bf-empty-state icon="history" message="Todavía no hay decisiones de moderación." />
    } @else {
      <ol class="historial">
        @for (evento of cola.items(); track evento.id) {
          <li>
            <mat-icon aria-hidden="true">{{ acciones[evento.accion].icono }}</mat-icon>
            <div>
              <p class="que">
                {{ acciones[evento.accion].texto }}
                @if (evento.recursoTipo === 'BANCO') {
                  · <a [routerLink]="['/bancos', evento.recursoId]">ver banco</a>
                }
              </p>
              @if (evento.motivo) {
                <p class="motivo">«{{ evento.motivo }}»</p>
              }
              <p class="meta">
                <time [attr.datetime]="evento.fecha">{{ evento.fecha | date: 'medium' }}</time>
                · por <a [routerLink]="['/usuarios', evento.moderadorId]">un moderador</a>
              </p>
            </div>
          </li>
        }
      </ol>
      @if (cola.cargando()) {
        <bf-loading-state />
      } @else if (cola.hayMas()) {
        <button mat-stroked-button type="button" (click)="cola.cargarMas()">Cargar más</button>
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

  protected readonly acciones = ACCIONES;
  protected readonly cola = new Cola<EventoModeracion>((page) => this.api.eventos(page), 30);

  ngOnInit(): void {
    void this.cola.cargar();
  }
}
