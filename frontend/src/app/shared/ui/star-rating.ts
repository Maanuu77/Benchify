import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

/**
 * Puntuación de 1 a 5 estrellas. En modo lectura muestra medias estrellas; en modo edición es
 * un grupo de radio accesible con teclado (flechas) y lectores de pantalla.
 */
@Component({
  selector: 'bf-star-rating',
  imports: [MatIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (editable()) {
      <div class="stars" role="radiogroup" [attr.aria-label]="label()">
        @for (star of stars; track star) {
          <button
            type="button"
            role="radio"
            class="star"
            [attr.aria-checked]="value() === star"
            [attr.aria-label]="star + (star === 1 ? ' estrella' : ' estrellas')"
            [attr.tabindex]="value() === star || (value() === 0 && star === 1) ? 0 : -1"
            (click)="value.set(star)"
            (keydown)="onKeydown($event)"
          >
            <mat-icon aria-hidden="true">{{ star <= value() ? 'star' : 'star_border' }}</mat-icon>
          </button>
        }
      </div>
    } @else {
      <span class="stars" role="img" [attr.aria-label]="readLabel()">
        @for (icon of icons(); track $index) {
          <mat-icon aria-hidden="true">{{ icon }}</mat-icon>
        }
      </span>
    }
  `,
  styles: `
    .stars {
      display: inline-flex;
      align-items: center;
      color: var(--mat-sys-tertiary);
    }
    .star {
      border: 0;
      background: none;
      padding: 4px;
      cursor: pointer;
      color: inherit;
      border-radius: 50%;
    }
    .star:focus-visible {
      outline: 2px solid var(--mat-sys-primary);
    }
  `,
})
export class StarRating {
  readonly value = model(0);
  readonly editable = input(false);
  readonly label = input('Puntuación');

  protected readonly stars = [1, 2, 3, 4, 5];

  protected readonly icons = computed(() =>
    this.stars.map((star) => {
      const v = this.value();
      if (v >= star) return 'star';
      if (v >= star - 0.5) return 'star_half';
      return 'star_border';
    }),
  );

  protected readonly readLabel = computed(() => `${this.value().toLocaleString('es-ES')} de 5 estrellas`);

  protected onKeydown(event: KeyboardEvent): void {
    const delta = event.key === 'ArrowRight' || event.key === 'ArrowUp' ? 1
      : event.key === 'ArrowLeft' || event.key === 'ArrowDown' ? -1 : 0;
    if (delta === 0) return;
    event.preventDefault();
    const next = Math.min(5, Math.max(1, (this.value() || 0) + delta));
    this.value.set(next);
    const group = (event.currentTarget as HTMLElement).closest('[role="radiogroup"]');
    group?.querySelectorAll<HTMLButtonElement>('button')[next - 1]?.focus();
  }
}
