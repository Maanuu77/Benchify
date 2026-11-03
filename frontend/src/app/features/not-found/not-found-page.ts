import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { textos } from '../../core/i18n/idioma';
import { EmptyState } from '../../shared/ui/empty-state';
import { NOT_FOUND } from './not-found-page.i18n';

@Component({
  selector: 'bf-not-found-page',
  imports: [EmptyState, MatButton, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page">
      <h1>{{ t.titulo }}</h1>
      <bf-empty-state icon="explore_off" [message]="t.mensaje">
        <a mat-flat-button routerLink="/mapa">{{ t.volver }}</a>
      </bf-empty-state>
    </section>
  `,
  styles: `
    .page {
      max-width: 640px;
      margin: 0 auto;
      padding: 24px 16px;
      text-align: center;
    }
  `,
})
export class NotFoundPage {
  protected readonly t = textos(NOT_FOUND);
}
