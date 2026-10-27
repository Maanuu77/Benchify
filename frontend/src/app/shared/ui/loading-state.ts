import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

/** Indicador de carga accesible: los lectores de pantalla anuncian el mensaje. */
@Component({
  selector: 'bf-loading-state',
  imports: [MatProgressSpinner],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="state" role="status" aria-live="polite">
      <mat-spinner [diameter]="diameter()" aria-hidden="true" />
      <span>{{ message() }}</span>
    </div>
  `,
  styles: `
    .state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 32px 16px;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class LoadingState {
  readonly message = input('Cargando…');
  readonly diameter = input(40);
}
