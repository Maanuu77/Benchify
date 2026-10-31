import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

@Component({
  selector: 'bf-empty-state',
  imports: [MatIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="state">
      <span class="icono" aria-hidden="true"><mat-icon>{{ icon() }}</mat-icon></span>
      <p>{{ message() }}</p>
      <ng-content />
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
      color: var(--bf-ink-2);
    }
    .icono {
      display: grid;
      place-items: center;
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background: var(--bf-accent-soft);
      color: var(--bf-accent-ink);

      mat-icon {
        width: 30px;
        height: 30px;
        font-size: 30px;
      }
    }
    p {
      margin: 0;
      max-width: 34ch;
    }
  `,
})
export class EmptyState {
  readonly message = input.required<string>();
  readonly icon = input('inbox');
}
