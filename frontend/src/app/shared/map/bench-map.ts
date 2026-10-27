import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import type * as Leaflet from 'leaflet';
import { BancoResumen, BoundingBox } from '../../core/api/models';
import { cargarLeaflet, iconoBanco, LeafletLib, TILE_ATTRIBUTION, TILE_URL } from './leaflet-loader';

export interface Punto {
  lat: number;
  lng: number;
}

/** Mapa de Leaflet con marcadores agrupados. Emite el área visible cada vez que el usuario la mueve. */
@Component({
  selector: 'bf-bench-map',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div #mapa class="mapa" role="region" [attr.aria-label]="ariaLabel()"></div>`,
  styles: `
    :host {
      display: block;
      position: relative;
    }
    .mapa {
      height: 100%;
      min-height: 320px;
    }
  `,
})
export class BenchMap {
  readonly bancos = input<BancoResumen[]>([]);
  readonly centro = input.required<Punto>();
  readonly zoom = input(14);
  /** Posición del usuario, si la ha compartido. */
  readonly usuario = input<Punto | null>(null);
  readonly ariaLabel = input('Mapa de bancos');

  readonly areaChange = output<BoundingBox>();
  readonly bancoSelect = output<BancoResumen>();

  private readonly host = viewChild.required<ElementRef<HTMLElement>>('mapa');
  private readonly ready = signal(false);
  private L!: LeafletLib;
  private map?: Leaflet.Map;
  private cluster?: Leaflet.MarkerClusterGroup;
  private marcadorUsuario?: Leaflet.CircleMarker;

  constructor() {
    afterNextRender(() => void this.init());
    inject(DestroyRef).onDestroy(() => this.map?.remove());

    effect(() => {
      const bancos = this.bancos();
      if (this.ready()) {
        this.pintarBancos(bancos);
      }
    });

    effect(() => {
      const usuario = this.usuario();
      if (this.ready() && usuario) {
        this.pintarUsuario(usuario);
      }
    });
  }

  /** Centra el mapa en un punto (p. ej. al elegir un banco de la lista). */
  volarA(punto: Punto, zoom = 17): void {
    this.map?.flyTo([punto.lat, punto.lng], zoom, { duration: 0.6 });
  }

  private async init(): Promise<void> {
    this.L = await cargarLeaflet();
    const { lat, lng } = this.centro();
    this.map = this.L.map(this.host().nativeElement, { zoomControl: true }).setView([lat, lng], this.zoom());
    this.L.tileLayer(TILE_URL, { attribution: TILE_ATTRIBUTION, maxZoom: 19 }).addTo(this.map);
    this.cluster = this.L.markerClusterGroup({ showCoverageOnHover: false, maxClusterRadius: 50 });
    this.map.addLayer(this.cluster);
    this.map.on('moveend', () => this.emitirArea());
    this.ready.set(true);
    this.emitirArea();
  }

  private emitirArea(): void {
    const bounds = this.map?.getBounds();
    if (!bounds) return;
    this.areaChange.emit({
      minLat: bounds.getSouth(),
      minLng: bounds.getWest(),
      maxLat: bounds.getNorth(),
      maxLng: bounds.getEast(),
    });
  }

  private pintarBancos(bancos: BancoResumen[]): void {
    const L = this.L;
    this.cluster!.clearLayers();
    this.cluster!.addLayers(bancos.map((banco) => {
      const marker = L.marker([banco.lat, banco.lng], {
        icon: iconoBanco(L),
        title: banco.titulo,
        alt: banco.titulo,
        keyboard: true,
      });
      marker.on('click', () => this.bancoSelect.emit(banco));
      return marker;
    }));
  }

  private pintarUsuario({ lat, lng }: Punto): void {
    if (this.marcadorUsuario) {
      this.marcadorUsuario.setLatLng([lat, lng]);
    } else {
      this.marcadorUsuario = this.L.circleMarker([lat, lng], {
        radius: 8, weight: 3, color: '#ffffff', fillColor: '#1a73e8', fillOpacity: 1,
      }).bindTooltip('Estás aquí').addTo(this.map!);
    }
  }
}
