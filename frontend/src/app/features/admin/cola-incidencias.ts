import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { IncidenciasApi } from '../../core/api/incidencias-api';
import { Incidencia } from '../../core/api/models';
import { Notifier } from '../../core/ui/notifier';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { TIPOS_INCIDENCIA } from '../bancos/detalle/avisos-banco';
import { Cola } from './cola';

@Component({
  selector: 'bf-cola-incidencias',
  imports: [DatePipe, RouterLink, MatButton, MatIcon, LoadingState, ErrorState, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>Avisos de la comunidad ({{ cola.total() }})</h2>
    @if (cola.error(); as mensaje) {
      <bf-error-state [message]="mensaje" (retry)="cola.cargar()" />
    } @else if (!cola.cargando() && cola.items().length === 0) {
      <bf-empty-state icon="verified" message="No hay avisos abiertos." />
    } @else {
      <ul class="lista">
        @for (incidencia of cola.items(); track incidencia.id) {
          <li class="item">
            <div class="contenido">
              <p class="meta">
                <strong><mat-icon class="icono" aria-hidden="true">{{ tipos[incidencia.tipo].icono }}</mat-icon>
                  {{ tipos[incidencia.tipo].etiqueta }}</strong> ·
                {{ incidencia.fecha | date: 'short' }} ·
                <a [routerLink]="['/bancos', incidencia.bancoId]">ver banco</a>
              </p>
              <p>{{ incidencia.comentario ?? '(sin detalles)' }}</p>
            </div>
            <div class="acciones">
              <button mat-stroked-button type="button" [disabled]="ocupado() === incidencia.id"
                      (click)="cerrar(incidencia, 'DESCARTADA')">
                <mat-icon aria-hidden="true">close</mat-icon> Descartar
              </button>
              <button mat-flat-button type="button" [disabled]="ocupado() === incidencia.id"
                      (click)="cerrar(incidencia, 'RESUELTA')">
                <mat-icon aria-hidden="true">done</mat-icon> Resuelta
              </button>
            </div>
          </li>
        }
      </ul>
      @if (cola.cargando()) {
        <bf-loading-state />
      } @else if (cola.hayMas()) {
        <button mat-stroked-button type="button" (click)="cola.cargarMas()">Cargar más</button>
      }
    }
  `,
  styleUrl: './cola.scss',
  styles: `
    .icono {
      width: 18px;
      height: 18px;
      font-size: 18px;
      vertical-align: -4px;
    }
  `,
})
export class ColaIncidencias implements OnInit {
  private readonly api = inject(IncidenciasApi);
  private readonly notifier = inject(Notifier);

  protected readonly tipos = TIPOS_INCIDENCIA;
  protected readonly cola = new Cola<Incidencia>((page) => this.api.abiertas(page));
  protected readonly ocupado = signal<string | null>(null);

  ngOnInit(): void {
    void this.cola.cargar();
  }

  /** Quien avisó recibe una notificación con el resultado. */
  protected async cerrar(incidencia: Incidencia, estado: 'RESUELTA' | 'DESCARTADA'): Promise<void> {
    this.ocupado.set(incidencia.id);
    try {
      await firstValueFrom(this.api.cerrar(incidencia.id, estado));
      this.cola.quitar(incidencia.id);
      this.notifier.success(estado === 'RESUELTA' ? 'Aviso resuelto' : 'Aviso descartado');
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.ocupado.set(null);
    }
  }
}
