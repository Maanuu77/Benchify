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
import { anadirCapaBase, cargarLeaflet, iconoBanco, iconoGrupo, iconoUsuario, LeafletLib } from './leaflet-loader';

export interface Punto {
  lat: number;
  lng: number;
}

export interface Vista extends Punto {
  zoom: number;
}

/** Con menos zoom el área visible daría la vuelta al mundo. */
export const ZOOM_MINIMO = 3;

/** Leaflet puede devolver longitudes fuera de ±180 al desplazarse; la API las rechaza. */
function limitar(valor: number, maximo: number): number {
  return Math.max(-maximo, Math.min(maximo, valor));
}

/**
 * Mapa de Leaflet a pantalla completa con marcadores agrupados. Emite el área visible cada vez
 * que el usuario la mueve. Los controles (zoom, ubicación) los pone la página encima del mapa.
 */
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
      position: absolute;
      inset: 0;
    }
  `,
})
export class BenchMap {
  readonly bancos = input<BancoResumen[]>([]);
  readonly centro = input.required<Punto>();
  readonly zoom = input(14);
  /** Posición del usuario, si la ha compartido. */
  readonly usuario = input<Punto | null>(null);
  /** Banco resaltado en el mapa. */
  readonly seleccionado = input<string | null>(null);
  readonly ariaLabel = input('Mapa de bancos');
  /** Sin zoom con la rueda ni arrastre en móvil: para mapas dentro de una página que se desplaza. */
  readonly interactivo = input(true);

  readonly areaChange = output<BoundingBox>();
  /** Centro y zoom tras cada movimiento, para poder volver a la misma vista (p. ej. desde la URL). */
  readonly vistaChange = output<Vista>();
  readonly bancoSelect = output<BancoResumen>();

  private readonly host = viewChild.required<ElementRef<HTMLElement>>('mapa');
  private readonly ready = signal(false);
  private L!: LeafletLib;
  private map?: Leaflet.Map;
  private cluster?: Leaflet.MarkerClusterGroup;
  private marcadorUsuario?: Leaflet.Marker;
  private quitarCapaBase?: () => void;
  /** Marcadores pintados, por id de banco: al mover el mapa solo se añaden o quitan los que cambian. */
  private readonly marcadores = new Map<string, { banco: BancoResumen; marker: Leaflet.Marker }>();
  private seleccionadoPintado: string | null = null;

  constructor() {
    afterNextRender(() => void this.init());
    inject(DestroyRef).onDestroy(() => {
      this.quitarCapaBase?.();
      this.map?.remove();
    });

    effect(() => {
      const bancos = this.bancos();
      if (this.ready()) {
        this.pintarBancos(bancos);
      }
    });

    effect(() => {
      const id = this.seleccionado();
      if (this.ready()) {
        this.resaltar(id);
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

  acercar(): void {
    this.map?.zoomIn();
  }

  alejar(): void {
    this.map?.zoomOut();
  }

  private async init(): Promise<void> {
    this.L = await cargarLeaflet();
    const { lat, lng } = this.centro();
    // Sin zoom de "mundo entero" ni salir de él: el área pedida a la API siempre es válida
    this.map = this.L.map(this.host().nativeElement, {
      zoomControl: false,
      minZoom: ZOOM_MINIMO,
      maxBounds: [[-90, -180], [90, 180]],
      maxBoundsViscosity: 1,
      scrollWheelZoom: this.interactivo(),
      dragging: this.interactivo() || !this.L.Browser.mobile,
    }).setView([lat, lng], this.zoom());
    this.quitarCapaBase = anadirCapaBase(this.L, this.map);
    this.cluster = this.L.markerClusterGroup({
      showCoverageOnHover: false,
      maxClusterRadius: 50,
      iconCreateFunction: (cluster) => iconoGrupo(this.L, cluster),
    });
    this.map.addLayer(this.cluster);
    this.map.on('moveend', () => this.emitirArea());
    this.ready.set(true);
    this.emitirArea();
  }

  private emitirArea(): void {
    const bounds = this.map?.getBounds();
    if (!bounds) return;
    const centro = this.map!.getCenter();
    this.vistaChange.emit({ lat: centro.lat, lng: centro.lng, zoom: this.map!.getZoom() });
    this.areaChange.emit({
      minLat: limitar(bounds.getSouth(), 90),
      minLng: limitar(bounds.getWest(), 180),
      maxLat: limitar(bounds.getNorth(), 90),
      maxLng: limitar(bounds.getEast(), 180),
    });
  }

  private pintarBancos(bancos: BancoResumen[]): void {
    const nuevos = new Map(bancos.map((banco) => [banco.id, banco]));
    const quitar: Leaflet.Marker[] = [];
    for (const [id, { banco, marker }] of this.marcadores) {
      const actual = nuevos.get(id);
      if (!actual || actual.lat !== banco.lat || actual.lng !== banco.lng) {
        quitar.push(marker);
        this.marcadores.delete(id);
      }
    }
    const anadir = bancos
      .filter((banco) => !this.marcadores.has(banco.id))
      .map((banco) => {
        const marker = this.L.marker([banco.lat, banco.lng], {
          icon: iconoBanco(this.L, banco.id === this.seleccionado()),
          title: banco.titulo,
          alt: banco.titulo,
          keyboard: true,
        });
        marker.on('click', () => this.bancoSelect.emit(this.marcadores.get(banco.id)?.banco ?? banco));
        this.marcadores.set(banco.id, { banco, marker });
        return marker;
      });
    // Datos actualizados (puntuación…) para los que siguen en el mapa
    for (const [id, entrada] of this.marcadores) {
      entrada.banco = nuevos.get(id) ?? entrada.banco;
    }
    this.cluster!.removeLayers(quitar);
    this.cluster!.addLayers(anadir);
    this.seleccionadoPintado = this.seleccionado();
  }

  private resaltar(id: string | null): void {
    if (id === this.seleccionadoPintado) return;
    const anterior = this.seleccionadoPintado ? this.marcadores.get(this.seleccionadoPintado) : undefined;
    anterior?.marker.setIcon(iconoBanco(this.L));
    anterior?.marker.setZIndexOffset(0);
    const actual = id ? this.marcadores.get(id) : undefined;
    actual?.marker.setIcon(iconoBanco(this.L, true));
    actual?.marker.setZIndexOffset(1000);
    this.seleccionadoPintado = id;
  }

  private pintarUsuario({ lat, lng }: Punto): void {
    if (this.marcadorUsuario) {
      this.marcadorUsuario.setLatLng([lat, lng]);
    } else {
      this.marcadorUsuario = this.L.marker([lat, lng], {
        icon: iconoUsuario(this.L),
        title: 'Estás aquí',
        keyboard: false,
        zIndexOffset: 2000,
      }).addTo(this.map!);
    }
  }
}
