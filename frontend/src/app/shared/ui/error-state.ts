import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';

/** Error recuperable con botón de reintento. */
@Component({
  selector: 'bf-error-state',
  imports: [MatButton, MatIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="state" role="alert">
      <span class="icono" aria-hidden="true"><mat-icon>cloud_off</mat-icon></span>
      <p>{{ message() }}</p>
      @if (retryable()) {
        <button matButton="tonal" type="button" (click)="retry.emit()"><mat-icon>refresh</mat-icon> Reintentar</button>
      }
    </div>
  `,
  styles: `
    .state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 32px 16px;
      text-align: center;
    }
    .icono {
      display: grid;
      place-items: center;
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background: color-mix(in srgb, var(--bf-danger) 12%, transparent);
      color: var(--bf-danger);

      mat-icon {
        width: 30px;
        height: 30px;
        font-size: 30px;
      }
    }
    p {
      margin: 0;
      max-width: 34ch;
      color: var(--bf-ink);
    }
  `,
})
export class ErrorState {
  readonly message = input('Algo ha salido mal.');
  readonly retryable = input(true);
  readonly retry = output<void>();
}
