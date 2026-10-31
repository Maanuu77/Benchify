import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  model,
  signal,
  viewChild,
} from '@angular/core';
import type * as Leaflet from 'leaflet';
import { Punto } from './bench-map';
import { anadirCapaBase, cargarLeaflet, iconoBanco, LeafletLib } from './leaflet-loader';

/**
 * Mapa para elegir dónde está un banco: clic en el mapa o arrastrar el marcador.
 * Las coordenadas también se pueden escribir en el formulario, así que no depende solo del ratón.
 */
@Component({
  selector: 'bf-location-picker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div #mapa class="mapa" role="application" aria-label="Mapa para marcar la ubicación del banco"></div>`,
  styles: `
    .mapa {
      height: 100%;
      min-height: 280px;
      border-radius: 12px;
    }
  `,
})
export class LocationPicker {
  readonly ubicacion = model<Punto | null>(null);
  readonly centro = input.required<Punto>();
  readonly zoom = input(16);

  private readonly host = viewChild.required<ElementRef<HTMLElement>>('mapa');
  private readonly ready = signal(false);
  private L!: LeafletLib;
  private map?: Leaflet.Map;
  private quitarCapaBase?: () => void;
  private marker?: Leaflet.Marker;

  constructor() {
    afterNextRender(() => void this.init());
    inject(DestroyRef).onDestroy(() => {
      this.quitarCapaBase?.();
      this.map?.remove();
    });

    effect(() => {
      const punto = this.ubicacion();
      if (this.ready() && punto) {
        this.colocarMarcador(punto);
      }
    });
  }

  private async init(): Promise<void> {
    this.L = await cargarLeaflet();
    const inicial = this.ubicacion() ?? this.centro();
    this.map = this.L.map(this.host().nativeElement).setView([inicial.lat, inicial.lng], this.zoom());
    this.quitarCapaBase = anadirCapaBase(this.L, this.map);
    this.map.on('click', (event: Leaflet.LeafletMouseEvent) => this.ubicacion.set(redondear(event.latlng)));
    this.ready.set(true);
  }

  private colocarMarcador(punto: Punto): void {
    if (!this.marker) {
      this.marker = this.L.marker([punto.lat, punto.lng], {
        draggable: true, icon: iconoBanco(this.L, true), title: 'Ubicación del banco', keyboard: true,
      }).addTo(this.map!);
      this.marker.on('dragend', () => this.ubicacion.set(redondear(this.marker!.getLatLng())));
    } else {
      this.marker.setLatLng([punto.lat, punto.lng]);
    }
    if (!this.map!.getBounds().contains([punto.lat, punto.lng])) {
      this.map!.panTo([punto.lat, punto.lng]);
    }
  }
}

/** 6 decimales ≈ 10 cm: más precisión no tiene sentido para un banco. */
export function redondear({ lat, lng }: Punto): Punto {
  return { lat: Math.round(lat * 1e6) / 1e6, lng: Math.round(lng * 1e6) / 1e6 };
}
