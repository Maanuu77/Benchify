import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal, signal, viewChild } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatListItem, MatListItemLine, MatListItemTitle, MatNavList } from '@angular/material/list';
import { RouterLink } from '@angular/router';
import { combineLatest, debounceTime, filter, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Loadable, loadable } from '../../core/api/api-error';
import { BancosApi } from '../../core/api/bancos-api';
import { BancoCercano, BancoResumen, BoundingBox, FiltroBancos, Page } from '../../core/api/models';
import { GeolocationService } from '../../core/geo/geolocation.service';
import { Notifier } from '../../core/ui/notifier';
import { BenchMap, Punto } from '../../shared/map/bench-map';
import { DistanciaPipe } from '../../shared/ui/distancia.pipe';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { StarRating } from '../../shared/ui/star-rating';
import { FiltrosBancos } from './filtros-bancos';

/** Radio de la búsqueda "cerca de mí", en metros. */
export const RADIO_CERCANIA = 1500;

@Component({
  selector: 'bf-mapa-page',
  imports: [
    BenchMap, FiltrosBancos, StarRating, LoadingState, ErrorState, EmptyState, DistanciaPipe, DecimalPipe,
    RouterLink, MatButton, MatIcon, MatNavList, MatListItem, MatListItemTitle, MatListItemLine,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './mapa-page.html',
  styleUrl: './mapa-page.scss',
})
export class MapaPage {
  private readonly api = inject(BancosApi);
  private readonly geo = inject(GeolocationService);
  private readonly notifier = inject(Notifier);
  private readonly mapa = viewChild(BenchMap);

  protected readonly centroInicial: Punto = { lat: environment.map.center[0], lng: environment.map.center[1] };
  protected readonly zoomInicial = environment.map.zoom;

  protected readonly area = signal<BoundingBox | null>(null);
  protected readonly filtro = signal<FiltroBancos>({});
  protected readonly usuario = signal<Punto | null>(null);
  protected readonly seleccionado = signal<BancoResumen | null>(null);
  protected readonly localizando = signal(false);
  protected readonly cercanos = signal<Loadable<Page<BancoCercano>> | null>(null);

  /** Bancos del área visible; se vuelve a pedir al mover el mapa o cambiar filtros. */
  protected readonly estado = toSignal(
    combineLatest([
      toObservable(this.area).pipe(filter((a): a is BoundingBox => a !== null), debounceTime(250)),
      toObservable(this.filtro),
    ]).pipe(switchMap(([area, filtro]) => loadable(this.api.enArea(area, filtro)))),
    { initialValue: { status: 'loading' } as Loadable<BancoResumen[]> },
  );

  /** Conserva los últimos marcadores mientras carga la siguiente área para que no parpadeen. */
  protected readonly bancos = linkedSignal<Loadable<BancoResumen[]>, BancoResumen[]>({
    source: this.estado,
    computation: (estado, anterior) => (estado.status === 'success' ? estado.value : (anterior?.value ?? [])),
  });

  protected readonly mejorValorados = computed(() =>
    [...this.bancos()].sort((a, b) => b.puntuacionMedia - a.puntuacionMedia || b.numResenas - a.numResenas).slice(0, 20),
  );

  protected async buscarCerca(): Promise<void> {
    this.localizando.set(true);
    try {
      const posicion = await this.geo.posicionActual();
      this.usuario.set(posicion);
      this.mapa()?.volarA(posicion, 15);
      this.cargarCercanos(posicion);
    } catch (error) {
      this.notifier.error(error instanceof Error ? error.message : 'No hemos podido obtener tu ubicación.');
    } finally {
      this.localizando.set(false);
    }
  }

  protected cargarCercanos(posicion: Punto): void {
    loadable(this.api.cercanos(posicion.lat, posicion.lng, RADIO_CERCANIA, this.filtro(), { size: 20 }))
      .subscribe((estado) => this.cercanos.set(estado));
  }

  protected cerrarCercanos(): void {
    this.cercanos.set(null);
  }

  protected enfocar(banco: BancoResumen): void {
    this.seleccionado.set(banco);
    this.mapa()?.volarA(banco);
  }
}
