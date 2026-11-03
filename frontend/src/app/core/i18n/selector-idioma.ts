import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { cambiarIdioma, IDIOMA } from './idioma';

/** Cambia entre español e inglés; la página se recarga en el idioma nuevo. */
@Component({
  selector: 'bf-selector-idioma',
  imports: [MatIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button type="button" class="idioma" [attr.lang]="otro.codigo" [attr.aria-label]="otro.accion" [title]="otro.accion"
            (click)="cambiar()">
      <mat-icon aria-hidden="true">translate</mat-icon>
      <span aria-hidden="true">{{ otro.corto }}</span>
    </button>
  `,
  styles: `
    .idioma {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      height: 36px;
      padding: 0 10px;
      border: 0;
      border-radius: var(--bf-r-pill);
      background: none;
      color: var(--bf-ink-2);
      font: 600 13px var(--bf-font);
      cursor: pointer;

      &:hover {
        background: var(--bf-surface-2);
        color: var(--bf-ink);
      }
      mat-icon {
        width: 18px;
        height: 18px;
        font-size: 18px;
      }
    }
  `,
})
export class SelectorIdioma {
  private readonly idioma = inject(IDIOMA);

  /** Se ofrece el otro idioma, escrito en ese idioma para que lo reconozca quien lo habla. */
  protected readonly otro = this.idioma === 'es'
    ? { codigo: 'en', corto: 'EN', accion: 'Switch to English' }
    : { codigo: 'es', corto: 'ES', accion: 'Cambiar a español' };

  protected cambiar(): void {
    cambiarIdioma(this.otro.codigo === 'en' ? 'en' : 'es');
  }
}
