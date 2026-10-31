import { BreakpointObserver } from '@angular/cdk/layout';
import { DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { combineLatest, debounceTime, filter, map, of, skip, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Loadable, loadable } from '../../core/api/api-error';
import { BancosApi, MAX_RESULTADOS_MAPA } from '../../core/api/bancos-api';
import { BancoCercano, BancoResumen, BoundingBox, FiltroBancos, Page } from '../../core/api/models';
import { GeolocationService } from '../../core/geo/geolocation.service';
import { BREAKPOINT_MOVIL } from '../../core/layout/shell';
import { Notifier } from '../../core/ui/notifier';
import { BenchMap, Punto, Vista } from '../../shared/map/bench-map';
import { distanciaMetros } from '../../shared/map/distancia';
import { DistanciaPipe } from '../../shared/ui/distancia.pipe';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { aQueryParams, leerEstadoMapa } from './estado-mapa';
import { FiltrosBancos } from './filtros-bancos';

/** Radio de la búsqueda "cerca de mí", en metros. */
export const RADIO_CERCANIA = 1500;

/** Banco de la lista con la distancia al usuario, si se conoce. */
export interface ElementoLista {
  banco: BancoResumen;
  distancia: number | null;
}

@Component({
  selector: 'bf-mapa-page',
  imports: [
    BenchMap, FiltrosBancos, LoadingState, ErrorState, EmptyState, DistanciaPipe, DecimalPipe, NgTemplateOutlet,
    RouterLink, MatButton, MatIcon,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './mapa-page.html',
  styleUrl: './mapa-page.scss',
})
export class MapaPage {
  private readonly api = inject(BancosApi);
  private readonly geo = inject(GeolocationService);
  private readonly notifier = inject(Notifier);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly mapa = viewChild(BenchMap);

  private readonly inicial = leerEstadoMapa(this.route.snapshot.queryParams);
  protected readonly centroInicial: Punto = this.inicial.vista ?? { lat: environment.map.center[0], lng: environment.map.center[1] };
  protected readonly zoomInicial = this.inicial.vista?.zoom ?? environment.map.zoom;

  protected readonly movil = toSignal(
    inject(BreakpointObserver).observe(BREAKPOINT_MOVIL).pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected readonly area = signal<BoundingBox | null>(null);
  private readonly vista = signal<Vista | null>(this.inicial.vista);
  protected readonly filtro = signal<FiltroBancos>(this.inicial.filtro);
  protected readonly usuario = signal<Punto | null>(null);
  protected readonly seleccionado = signal<BancoResumen | null>(null);
  protected readonly localizando = signal(false);
  /** Hoja inferior desplegada (solo móvil). */
  protected readonly hojaAbierta = signal(false);
  /** Punto de la búsqueda "cerca de mí"; null cuando la lista de cercanos está cerrada. */
  private readonly origenCercanos = signal<Punto | null>(null);

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

  /** Bancos cercanos: se vuelven a pedir al cambiar los filtros y solo cuenta la última respuesta. */
  protected readonly cercanos = toSignal(
    combineLatest([toObservable(this.origenCercanos), toObservable(this.filtro)]).pipe(
      switchMap(([origen, filtro]) =>
        origen ? loadable(this.api.cercanos(origen.lat, origen.lng, RADIO_CERCANIA, filtro, { size: 20 })) : of(null),
      ),
    ),
    { initialValue: null as Loadable<Page<BancoCercano>> | null },
  );

  /** La API devuelve como mucho este número de bancos por área: si se llega, hay más sin mostrar. */
  protected readonly enLimite = computed(() => this.bancos().length >= MAX_RESULTADOS_MAPA);

  /** Los del área, mejor valorados primero, con la distancia si el usuario ha compartido su posición. */
  protected readonly mejorValorados = computed<ElementoLista[]>(() => {
    const usuario = this.usuario();
    return [...this.bancos()]
      .sort((a, b) => b.puntuacionMedia - a.puntuacionMedia || b.numResenas - a.numResenas)
      .slice(0, 20)
      .map((banco) => ({ banco, distancia: usuario ? distanciaMetros(usuario, banco) : null }));
  });

  constructor() {
    // Vista y filtros a la URL (sin crear entradas de historial) para poder volver y compartir
    combineLatest([toObservable(this.vista), toObservable(this.filtro)])
      .pipe(skip(1), debounceTime(400), takeUntilDestroyed())
      .subscribe(([vista, filtro]) =>
        void this.router.navigate([], {
          relativeTo: this.route,
          queryParams: aQueryParams({ vista, filtro }),
          queryParamsHandling: 'merge',
          replaceUrl: true,
        }),
      );
  }

  protected cambiarVista(vista: Vista): void {
    this.vista.set(vista);
  }

  protected async buscarCerca(): Promise<void> {
    this.localizando.set(true);
    try {
      const posicion = await this.geo.posicionActual();
      this.usuario.set(posicion);
      this.mapa()?.volarA(posicion, 15);
      this.cargarCercanos(posicion);
      this.hojaAbierta.set(true);
    } catch (error) {
      this.notifier.error(error instanceof Error ? error.message : 'No hemos podido obtener tu ubicación.');
    } finally {
      this.localizando.set(false);
    }
  }

  protected cargarCercanos(posicion: Punto): void {
    // Un objeto nuevo aunque sea el mismo punto: así "reintentar" vuelve a pedir la lista
    this.origenCercanos.set({ ...posicion });
  }

  protected cerrarCercanos(): void {
    this.origenCercanos.set(null);
  }

  /** Vuelve a pedir los bancos del área visible tras un error. */
  protected reintentarArea(): void {
    const area = this.area();
    if (area) {
      this.area.set({ ...area });
    }
  }

  /**
   * Al recorrer la lista con el teclado solo se selecciona el banco: mover el mapa cambiaría el
   * área y con ella la propia lista que se está recorriendo.
   */
  protected enfocar(banco: BancoResumen): void {
    this.seleccionado.set(banco);
  }

  /** Al tocar un marcador se muestra su tarjeta; en móvil, con la hoja plegada para seguir viendo el mapa. */
  protected elegirEnMapa(banco: BancoResumen): void {
    this.seleccionado.set(banco);
  }

  protected cerrarSeleccion(): void {
    this.seleccionado.set(null);
  }

  protected alternarHoja(): void {
    this.hojaAbierta.update((abierta) => !abierta);
  }

  protected acercar(): void {
    this.mapa()?.acercar();
  }

  protected alejar(): void {
    this.mapa()?.alejar();
  }

  protected comoLlegar(banco: BancoResumen): string {
    return `https://www.google.com/maps/dir/?api=1&destination=${banco.lat},${banco.lng}`;
  }

  protected distanciaA(banco: BancoResumen): number | null {
    const usuario = this.usuario();
    return usuario ? distanciaMetros(usuario, banco) : null;
  }
}
