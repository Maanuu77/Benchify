import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

/** Indicador de carga accesible: los lectores de pantalla anuncian el mensaje. */
@Component({
  selector: 'bf-loading-state',
  imports: [MatProgressSpinner],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (esqueleto() > 0) {
      <div class="esqueleto" role="status" aria-live="polite">
        <span class="sr-only">{{ message() }}</span>
        @for (fila of filas(); track fila) {
          <div class="fila" aria-hidden="true">
            <span class="cuadro"></span>
            <span class="lineas"><span></span><span></span></span>
          </div>
        }
      </div>
    } @else {
      <div class="state" role="status" aria-live="polite">
        <mat-spinner [diameter]="diameter()" aria-hidden="true" />
        <span>{{ message() }}</span>
      </div>
    }
  `,
  styles: `
    .state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 32px 16px;
      color: var(--bf-ink-2);
    }
    .esqueleto {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 4px 0;
    }
    .fila {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 8px;
    }
    .cuadro,
    .lineas span {
      border-radius: 8px;
      background: linear-gradient(90deg, var(--bf-surface-2) 25%, var(--bf-line) 50%, var(--bf-surface-2) 75%) 0 0 / 200% 100%;
      animation: brillo 1.4s ease-in-out infinite;
    }
    .cuadro {
      flex: none;
      width: 56px;
      height: 56px;
      border-radius: var(--bf-r-sm);
    }
    .lineas {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8px;

      span {
        height: 12px;
      }
      span:first-child {
        width: 70%;
        height: 14px;
      }
      span:last-child {
        width: 45%;
      }
    }
    @keyframes brillo {
      to {
        background-position: -200% 0;
      }
    }
  `,
})
export class LoadingState {
  readonly message = input('Cargando…');
  readonly diameter = input(40);
  /** Número de filas de esqueleto; 0 muestra el spinner. */
  readonly esqueleto = input(0);

  protected readonly filas = computed(() => Array.from({ length: this.esqueleto() }, (_, i) => i));
}
