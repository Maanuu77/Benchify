import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { firstValueFrom } from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { LugaresApi } from '../../core/api/lugares-api';
import { Lugar } from '../../core/api/models';
import { Punto } from '../../shared/map/bench-map';

export const MIN_BUSQUEDA = 3;

/**
 * Buscador de direcciones del mapa. Busca al pulsar Intro y no mientras se escribe: el servicio de
 * OpenStreetMap no admite autocompletado.
 */
@Component({
  selector: 'bf-buscador-lugares',
  imports: [MatIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="caja bf-glass" role="search" (submit)="$event.preventDefault(); buscar()">
      <mat-icon class="lupa" aria-hidden="true">search</mat-icon>
      <input
        #campo
        type="search"
        name="q"
        placeholder="Buscar una calle, un barrio o un sitio"
        aria-label="Buscar una dirección"
        autocomplete="off"
        enterkeyhint="search"
        maxlength="120"
        [value]="texto()"
        (input)="texto.set(campo.value)"
        (keydown.escape)="cerrar()"
        [attr.aria-expanded]="abierto()"
        aria-controls="resultados-lugares"
      />
      @if (buscando()) {
        <mat-icon class="estado girando" aria-hidden="true">autorenew</mat-icon>
      } @else if (texto()) {
        <button type="button" class="limpiar" aria-label="Borrar la búsqueda" (click)="limpiar(); campo.focus()">
          <mat-icon>close</mat-icon>
        </button>
      }
    </form>

    @if (abierto()) {
      <div id="resultados-lugares" class="resultados bf-glass" role="region" aria-live="polite" aria-label="Resultados">
        @if (error(); as mensaje) {
          <p class="vacio">{{ mensaje }}</p>
        } @else if (resultados()?.length === 0) {
          <p class="vacio">No hemos encontrado ese sitio. Prueba con la calle y la ciudad.</p>
        } @else {
          <ul>
            @for (lugar of resultados(); track $index) {
              <li>
                <button type="button" (click)="elegir(lugar)">
                  <mat-icon aria-hidden="true">place</mat-icon>
                  <span>
                    <b>{{ lugar.nombre }}</b>
                    <small>{{ lugar.direccion }}</small>
                  </span>
                </button>
              </li>
            }
          </ul>
        }
        <p class="atribucion">Datos © OpenStreetMap</p>
      </div>
    }
  `,
  styles: `
    :host {
      position: relative;
      display: block;
    }
    .caja {
      display: flex;
      align-items: center;
      gap: 8px;
      height: 46px;
      padding: 0 6px 0 14px;
      border-radius: var(--bf-r-pill);
      box-shadow: var(--bf-sh-1);

      &:focus-within {
        outline: 2px solid var(--bf-accent);
        outline-offset: 1px;
      }
    }
    .lupa {
      color: var(--bf-ink-2);
    }
    input {
      flex: 1;
      min-width: 0;
      height: 100%;
      border: 0;
      outline: 0;
      background: none;
      color: var(--bf-ink);
      font: 500 15px var(--bf-font);

      &::placeholder {
        color: var(--bf-ink-2);
      }
      &::-webkit-search-cancel-button {
        display: none;
      }
    }
    .limpiar {
      display: grid;
      place-items: center;
      width: 34px;
      height: 34px;
      border: 0;
      border-radius: 50%;
      background: none;
      color: var(--bf-ink-2);
      cursor: pointer;

      &:hover {
        background: var(--bf-surface-2);
      }
      mat-icon {
        width: 20px;
        height: 20px;
        font-size: 20px;
      }
    }
    .estado {
      margin-right: 8px;
      color: var(--bf-accent-ink);
    }
    .girando {
      animation: girar 900ms linear infinite;
    }
    @keyframes girar {
      to {
        transform: rotate(360deg);
      }
    }
    .resultados {
      position: absolute;
      top: calc(100% + 6px);
      left: 0;
      right: 0;
      z-index: 1;
      padding: 6px;
      border-radius: var(--bf-r);
      box-shadow: var(--bf-sh-2);
    }
    ul {
      margin: 0;
      padding: 0;
      list-style: none;
    }
    li button {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      width: 100%;
      padding: 10px;
      border: 0;
      border-radius: var(--bf-r-sm);
      background: none;
      color: var(--bf-ink);
      text-align: left;
      cursor: pointer;

      &:hover,
      &:focus-visible {
        background: var(--bf-surface-2);
      }
      mat-icon {
        flex: none;
        color: var(--bf-accent-ink);
      }
      span {
        display: flex;
        flex-direction: column;
        min-width: 0;
      }
      b {
        font: 600 14px/1.3 var(--bf-font);
      }
      small {
        overflow: hidden;
        color: var(--bf-ink-2);
        font-size: 12px;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
    .vacio {
      margin: 0;
      padding: 10px;
      color: var(--bf-ink-2);
      font-size: 14px;
    }
    .atribucion {
      margin: 4px 10px 2px;
      color: var(--bf-ink-2);
      font-size: 11px;
      text-align: right;
    }
  `,
})
export class BuscadorLugares {
  private readonly api = inject(LugaresApi);

  /** Centro del mapa: los resultados cercanos salen antes. */
  readonly cerca = input<Punto | null>(null);
  readonly lugarElegido = output<Lugar>();

  protected readonly texto = signal('');
  protected readonly buscando = signal(false);
  protected readonly resultados = signal<Lugar[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly abierto = signal(false);

  protected async buscar(): Promise<void> {
    const q = this.texto().trim();
    if (q.length < MIN_BUSQUEDA || this.buscando()) {
      return;
    }
    this.buscando.set(true);
    this.error.set(null);
    try {
      const lugares = await firstValueFrom(this.api.buscar(q, this.cerca()));
      // Con un único resultado no hace falta elegir
      if (lugares.length === 1) {
        this.elegir(lugares[0]);
        return;
      }
      this.resultados.set(lugares);
      this.abierto.set(true);
    } catch (error) {
      this.error.set(toApiError(error).message);
      this.abierto.set(true);
    } finally {
      this.buscando.set(false);
    }
  }

  protected elegir(lugar: Lugar): void {
    this.texto.set(lugar.nombre);
    this.cerrar();
    this.lugarElegido.emit(lugar);
  }

  protected limpiar(): void {
    this.texto.set('');
    this.cerrar();
  }

  protected cerrar(): void {
    this.abierto.set(false);
    this.resultados.set(null);
    this.error.set(null);
  }
}
