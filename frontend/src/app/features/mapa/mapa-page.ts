import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Página principal; el mapa de Leaflet llega en #27. */
@Component({
  selector: 'bf-mapa-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<h1 class="title">Mapa de bancos</h1>`,
  styles: `
    .title {
      padding: 16px;
    }
  `,
})
export class MapaPage {}
