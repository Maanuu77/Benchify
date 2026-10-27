import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { EmptyState } from '../../shared/ui/empty-state';

@Component({
  selector: 'bf-not-found-page',
  imports: [EmptyState, MatButton, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page">
      <h1>Página no encontrada</h1>
      <bf-empty-state icon="explore_off" message="Este banco se ha quedado sin sitio en el mapa.">
        <a mat-flat-button routerLink="/mapa">Volver al mapa</a>
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
export class NotFoundPage {}
