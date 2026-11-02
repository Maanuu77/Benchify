import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { DatosBancoModeracion, EdicionModeracion } from '../../core/api/models';
import { Notifier } from '../../core/ui/notifier';
import { Confirmation } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { MATERIALES } from '../bancos/detalle/banco-detalle-page';
import { Cola } from './cola';

export interface Cambio {
  campo: string;
  antes: string;
  despues: string;
}

const CAMPOS: { campo: string; valor: (d: DatosBancoModeracion) => string }[] = [
  { campo: 'Título', valor: (d) => d.titulo },
  { campo: 'Descripción', valor: (d) => d.descripcion ?? '' },
  { campo: 'Ubicación', valor: (d) => `${d.lat.toFixed(5)}, ${d.lng.toFixed(5)}` },
  { campo: 'Dirección', valor: (d) => d.direccion ?? '' },
  { campo: 'Ciudad', valor: (d) => d.ciudad ?? '' },
  { campo: 'Provincia', valor: (d) => d.provincia ?? '' },
  { campo: 'Sombra', valor: (d) => (d.conSombra ? 'Sí' : 'No') },
  { campo: 'Accesible', valor: (d) => (d.accesible ? 'Sí' : 'No') },
  { campo: 'Respaldo', valor: (d) => (d.conRespaldo ? 'Sí' : 'No') },
  { campo: 'Material', valor: (d) => (d.material ? MATERIALES[d.material] : '') },
  { campo: 'Etiquetas', valor: (d) => d.etiquetas.join(', ') },
];

/** Solo los campos que la edición cambia, con su valor publicado y el propuesto. */
export function cambios(edicion: EdicionModeracion): Cambio[] {
  return CAMPOS
    .map(({ campo, valor }) => ({ campo, antes: valor(edicion.actual), despues: valor(edicion.propuesta) }))
    .filter((c) => c.antes !== c.despues);
}

@Component({
  selector: 'bf-cola-ediciones',
  imports: [DatePipe, RouterLink, MatButton, MatIcon, LoadingState, ErrorState, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>Ediciones pendientes ({{ cola.total() }})</h2>
    <p class="ayuda">Bancos publicados que su autor ha cambiado. Siguen en el mapa como estaban hasta que decidas.</p>
    @if (cola.error(); as mensaje) {
      <bf-error-state [message]="mensaje" (retry)="cola.cargar()" />
    } @else if (!cola.cargando() && cola.items().length === 0) {
      <bf-empty-state icon="published_with_changes" message="No hay ediciones pendientes." />
    } @else {
      <ul class="lista">
        @for (edicion of cola.items(); track edicion.id) {
          <li class="item">
            <div class="contenido">
              <p class="meta">
                <strong>{{ edicion.actual.titulo }}</strong> ·
                {{ edicion.autorNombre ?? edicion.autorId }} · {{ edicion.fecha | date: 'short' }} ·
                <a [routerLink]="['/bancos', edicion.bancoId]">ver banco</a>
              </p>
              @let lista = cambios(edicion);
              @if (lista.length) {
                <table class="diff">
                  <thead><tr><th scope="col">Campo</th><th scope="col">Ahora</th><th scope="col">Propuesto</th></tr></thead>
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
                <p class="sin-cambios">No cambia nada respecto a lo publicado.</p>
              }
            </div>
            <div class="acciones">
              <button mat-stroked-button type="button" [disabled]="ocupado() === edicion.id" (click)="rechazar(edicion)">
                <mat-icon aria-hidden="true">close</mat-icon> Descartar
              </button>
              <button mat-flat-button type="button" [disabled]="ocupado() === edicion.id" (click)="aprobar(edicion)">
                <mat-icon aria-hidden="true">done</mat-icon> Aplicar
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

  protected readonly cola = new Cola<EdicionModeracion>((page) => this.api.ediciones(page));
  protected readonly ocupado = signal<string | null>(null);
  protected readonly cambios = cambios;

  ngOnInit(): void {
    void this.cola.cargar();
  }

  protected aprobar(edicion: EdicionModeracion): Promise<void> {
    return this.ejecutar(edicion, () => firstValueFrom(this.api.aprobarEdicion(edicion.id)), 'Edición aplicada');
  }

  protected async rechazar(edicion: EdicionModeracion): Promise<void> {
    const motivo = await this.confirmation.pedirMotivo({
      titulo: 'Descartar edición',
      mensaje: 'El banco se queda como está. Su autor recibirá un aviso.',
      confirmar: 'Descartar',
      sugerencias: ['Los datos no son correctos', 'Ubicación equivocada', 'Contenido inapropiado'],
    });
    if (motivo !== undefined) {
      await this.ejecutar(edicion, () => firstValueFrom(this.api.rechazarEdicion(edicion.id, motivo)), 'Edición descartada');
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
