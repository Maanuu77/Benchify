import { ChangeDetectionStrategy, Component, computed, inject, model } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIcon } from '@angular/material/icon';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { BancosApi } from '../../core/api/bancos-api';
import { FiltroBancos } from '../../core/api/models';

type Caracteristica = 'accesible' | 'conSombra' | 'conRespaldo';

export const VALORACIONES = [3, 4, 4.5];

/** Filtros del mapa como fila de píldoras: características que se activan y menús para etiqueta y valoración. */
@Component({
  selector: 'bf-filtros-bancos',
  imports: [MatIcon, MatMenu, MatMenuItem, MatMenuTrigger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pildoras" role="group" aria-label="Filtrar bancos">
      @for (c of caracteristicas; track c.clave) {
        <button type="button" class="pildora" [class.activa]="!!filtro()[c.clave]"
                [attr.aria-pressed]="!!filtro()[c.clave]" (click)="alternar(c.clave)">
          <mat-icon aria-hidden="true">{{ c.icono }}</mat-icon>{{ c.etiqueta }}
        </button>
      }
      <button type="button" class="pildora" [class.activa]="filtro().puntuacionMin !== undefined"
              [matMenuTriggerFor]="menuValoracion" aria-haspopup="menu">
        <mat-icon aria-hidden="true">star</mat-icon>
        {{ filtro().puntuacionMin !== undefined ? formatear(filtro().puntuacionMin!) + '+' : 'Valoración' }}
        <mat-icon aria-hidden="true" class="flecha">expand_more</mat-icon>
      </button>
      <button type="button" class="pildora" [class.activa]="filtro().etiqueta !== undefined"
              [matMenuTriggerFor]="menuEtiqueta" aria-haspopup="menu">
        <mat-icon aria-hidden="true">sell</mat-icon>
        {{ etiquetaActual() ?? 'Etiqueta' }}
        <mat-icon aria-hidden="true" class="flecha">expand_more</mat-icon>
      </button>
      @if (hayFiltros()) {
        <button type="button" class="pildora limpiar" (click)="filtro.set({})">
          <mat-icon aria-hidden="true">close</mat-icon>Quitar
        </button>
      }
    </div>

    <mat-menu #menuValoracion="matMenu">
      <button mat-menu-item type="button" (click)="actualizar({ puntuacionMin: undefined })">Cualquiera</button>
      @for (v of valoraciones; track v) {
        <button mat-menu-item type="button" (click)="actualizar({ puntuacionMin: v })">{{ formatear(v) }} o más</button>
      }
    </mat-menu>
    <mat-menu #menuEtiqueta="matMenu">
      <button mat-menu-item type="button" (click)="actualizar({ etiqueta: undefined })">Todas</button>
      @for (etiqueta of etiquetas(); track etiqueta.id) {
        <button mat-menu-item type="button" (click)="actualizar({ etiqueta: etiqueta.id })">{{ etiqueta.nombre }}</button>
      }
    </mat-menu>
  `,
  styles: `
    .pildoras {
      display: flex;
      gap: 8px;
      flex-wrap: var(--bf-filtros-wrap, wrap);
    }
    .pildora {
      flex: none;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 36px;
      padding: 0 14px;
      border-radius: var(--bf-r-pill);
      border: 1px solid var(--bf-line);
      background: var(--bf-pildora-fondo, var(--bf-surface-2));
      color: var(--bf-ink);
      font: 600 14px var(--bf-font);
      cursor: pointer;
      box-shadow: var(--bf-pildora-sombra, none);
      transition: background var(--bf-dur) var(--bf-ease), color var(--bf-dur) var(--bf-ease);

      mat-icon {
        width: 18px;
        height: 18px;
        font-size: 18px;
      }
      .flecha {
        margin-right: -6px;
        color: inherit;
      }
      &.activa {
        background: var(--bf-accent);
        border-color: transparent;
        color: var(--bf-on-accent);
      }
      &.limpiar {
        background: transparent;
        border-color: transparent;
        color: var(--bf-ink-2);
        box-shadow: none;
      }
    }
  `,
})
export class FiltrosBancos {
  readonly filtro = model<FiltroBancos>({});

  protected readonly etiquetas = toSignal(inject(BancosApi).etiquetas(), { initialValue: [] });
  protected readonly valoraciones = VALORACIONES;

  protected readonly caracteristicas: { clave: Caracteristica; etiqueta: string; icono: string }[] = [
    { clave: 'accesible', etiqueta: 'Accesible', icono: 'accessible' },
    { clave: 'conSombra', etiqueta: 'Sombra', icono: 'park' },
    { clave: 'conRespaldo', etiqueta: 'Respaldo', icono: 'chair' },
  ];

  protected readonly etiquetaActual = computed(() => {
    const id = this.filtro().etiqueta;
    return id == null ? null : (this.etiquetas().find((e) => e.id === id)?.nombre ?? null);
  });

  protected readonly hayFiltros = computed(() => Object.values(this.filtro()).some((v) => v != null && v !== false));

  protected alternar(clave: Caracteristica): void {
    this.actualizar({ [clave]: this.filtro()[clave] ? undefined : true });
  }

  protected actualizar(cambios: Partial<FiltroBancos>): void {
    this.filtro.update((actual) => {
      const nuevo: FiltroBancos = { ...actual, ...cambios };
      // Sin claves a undefined: el filtro se compara y se lleva a la URL tal cual
      for (const clave of Object.keys(nuevo) as (keyof FiltroBancos)[]) {
        if (nuevo[clave] == null) delete nuevo[clave];
      }
      return nuevo;
    });
  }

  protected formatear(valor: number): string {
    return valor.toLocaleString('es-ES');
  }
}
