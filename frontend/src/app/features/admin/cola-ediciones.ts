import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { DatosBancoModeracion, EdicionModeracion } from '../../core/api/models';
import { textos, textosActuales } from '../../core/i18n/idioma';
import { Notifier } from '../../core/ui/notifier';
import { Confirmation } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { MATERIALES } from '../bancos/detalle/banco-detalle-page';
import { COLA } from './admin.i18n';
import { Cola } from './cola';
import { COLA_EDICIONES } from './cola-ediciones.i18n';

export interface Cambio {
  campo: string;
  antes: string;
  despues: string;
}

type TextosEdiciones = typeof COLA_EDICIONES.es;

const CAMPOS: { campo: keyof TextosEdiciones['campos']; valor: (d: DatosBancoModeracion, t: TextosEdiciones) => string }[] = [
  { campo: 'titulo', valor: (d) => d.titulo },
  { campo: 'descripcion', valor: (d) => d.descripcion ?? '' },
  { campo: 'ubicacion', valor: (d) => `${d.lat.toFixed(5)}, ${d.lng.toFixed(5)}` },
  { campo: 'direccion', valor: (d) => d.direccion ?? '' },
  { campo: 'ciudad', valor: (d) => d.ciudad ?? '' },
  { campo: 'provincia', valor: (d) => d.provincia ?? '' },
  { campo: 'sombra', valor: (d, t) => (d.conSombra ? t.si : t.no) },
  { campo: 'accesible', valor: (d, t) => (d.accesible ? t.si : t.no) },
  { campo: 'respaldo', valor: (d, t) => (d.conRespaldo ? t.si : t.no) },
  { campo: 'material', valor: (d) => (d.material ? MATERIALES[d.material] : '') },
  { campo: 'etiquetas', valor: (d) => d.etiquetas.join(', ') },
];

/**
 * Solo los campos que la edición cambia, con su valor publicado y el propuesto. El componente pasa
 * sus textos; fuera de él se usan los del idioma de la visita.
 */
export function cambios(edicion: EdicionModeracion, t: TextosEdiciones = textosActuales(COLA_EDICIONES)): Cambio[] {
  return CAMPOS
    .map(({ campo, valor }) => ({
      campo: t.campos[campo], antes: valor(edicion.actual, t), despues: valor(edicion.propuesta, t),
    }))
    .filter((c) => c.antes !== c.despues);
}

@Component({
  selector: 'bf-cola-ediciones',
  imports: [DatePipe, RouterLink, MatButton, MatIcon, LoadingState, ErrorState, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>{{ t.titulo }} ({{ cola.total() }})</h2>
    <p class="ayuda">{{ t.ayuda }}</p>
    @if (cola.error(); as mensaje) {
      <bf-error-state [message]="mensaje" (retry)="cola.cargar()" />
    } @else if (!cola.cargando() && cola.items().length === 0) {
      <bf-empty-state icon="published_with_changes" [message]="t.vacia" />
    } @else {
      <ul class="lista">
        @for (edicion of cola.items(); track edicion.id) {
          <li class="item">
            <div class="contenido">
              <p class="meta">
                <strong>{{ edicion.actual.titulo }}</strong> ·
                {{ edicion.autorNombre ?? edicion.autorId }} · {{ edicion.fecha | date: 'short' }} ·
                <a [routerLink]="['/bancos', edicion.bancoId]">{{ tc.verBanco }}</a>
              </p>
              @let lista = cambios(edicion, t);
              @if (lista.length) {
                <table class="diff">
                  <thead><tr><th scope="col">{{ t.campo }}</th><th scope="col">{{ t.ahora }}</th><th scope="col">{{ t.propuesto }}</th></tr></thead>
                  <tbody>
                    @for (c of lista; track c.campo) {
                      <tr>
                        <th scope="row">{{ c.campo }}</th>
                        <td class="antes">{{ c.antes || '—' }}</td>
                        <td class="despues">{{ c.despues || '—' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              } @else {
                <p class="sin-cambios">{{ t.sinCambios }}</p>
              }
            </div>
            <div class="acciones">
              <button mat-stroked-button type="button" [disabled]="ocupado() === edicion.id" (click)="rechazar(edicion)">
                <mat-icon aria-hidden="true">close</mat-icon> {{ t.descartar }}
              </button>
              <button mat-flat-button type="button" [disabled]="ocupado() === edicion.id" (click)="aprobar(edicion)">
                <mat-icon aria-hidden="true">done</mat-icon> {{ t.aplicar }}
              </button>
            </div>
          </li>
        }
      </ul>
      @if (cola.cargando()) {
        <bf-loading-state />
      } @else if (cola.hayMas()) {
        <button mat-stroked-button type="button" (click)="cola.cargarMas()">{{ tc.cargarMas }}</button>
      }
    }
  `,
  styleUrl: './cola.scss',
  styles: `
    .ayuda {
      margin: -4px 0 16px;
      color: var(--bf-ink-2);
      font-size: 14px;
    }
    .diff {
      width: 100%;
      margin-top: 8px;
      border-collapse: collapse;
      font-size: 14px;

      th,
      td {
        padding: 6px 8px;
        border-bottom: 1px solid var(--bf-line);
        text-align: left;
        vertical-align: top;
      }
      thead th {
        color: var(--bf-ink-2);
        font-weight: 600;
        font-size: 12px;
      }
      tbody th {
        width: 110px;
        font-weight: 600;
      }
      .antes {
        color: var(--bf-ink-2);
        text-decoration: line-through;
      }
      .despues {
        background: var(--bf-accent-soft);
      }
    }
    .sin-cambios {
      color: var(--bf-ink-2);
    }
  `,
})
export class ColaEdiciones implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly notifier = inject(Notifier);
  private readonly confirmation = inject(Confirmation);
  protected readonly t = textos(COLA_EDICIONES);
  protected readonly tc = textos(COLA);

  protected readonly cola = new Cola<EdicionModeracion>((page) => this.api.ediciones(page));
  protected readonly ocupado = signal<string | null>(null);
  protected readonly cambios = cambios;

  ngOnInit(): void {
    void this.cola.cargar();
  }

  protected aprobar(edicion: EdicionModeracion): Promise<void> {
    return this.ejecutar(edicion, () => firstValueFrom(this.api.aprobarEdicion(edicion.id)), this.t.aplicada);
  }

  protected async rechazar(edicion: EdicionModeracion): Promise<void> {
    const motivo = await this.confirmation.pedirMotivo({
      titulo: this.t.descartarEdicion,
      mensaje: this.t.descartarMensaje,
      confirmar: this.t.descartar,
      sugerencias: this.t.sugerencias,
    });
    if (motivo !== undefined) {
      await this.ejecutar(edicion, () => firstValueFrom(this.api.rechazarEdicion(edicion.id, motivo)), this.t.descartada);
    }
  }

  private async ejecutar(edicion: EdicionModeracion, accion: () => Promise<unknown>, mensaje: string): Promise<void> {
    this.ocupado.set(edicion.id);
    try {
      await accion();
      this.cola.quitar(edicion.id);
      this.notifier.success(mensaje);
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.ocupado.set(null);
    }
  }
}
