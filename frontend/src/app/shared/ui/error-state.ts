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
      <mat-icon aria-hidden="true">error_outline</mat-icon>
      <p>{{ message() }}</p>
      @if (retryable()) {
        <button mat-stroked-button type="button" (click)="retry.emit()">Reintentar</button>
      }
    </div>
  `,
  styles: `
    .state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 32px 16px;
      text-align: center;
      color: var(--mat-sys-error);
    }
    p {
      margin: 0;
      color: var(--mat-sys-on-surface);
    }
  `,
})
export class ErrorState {
  readonly message = input('Algo ha salido mal.');
  readonly retryable = input(true);
  readonly retry = output<void>();
}
