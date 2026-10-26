import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { MatButton } from '@angular/material/button';
import { MatChip, MatChipSet } from '@angular/material/chips';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { combineLatest, firstValueFrom, switchMap } from 'rxjs';
import { Loadable, loadable } from '../../../core/api/api-error';
import { BancosApi } from '../../../core/api/bancos-api';
import { Banco, BancoResumen } from '../../../core/api/models';
import { UsuariosApi } from '../../../core/api/usuarios-api';
import { AuthService } from '../../../core/auth/auth.service';
import { GeolocationService } from '../../../core/geo/geolocation.service';
import { Notifier } from '../../../core/ui/notifier';
import { BenchMap } from '../../../shared/map/bench-map';
import { Confirmation } from '../../../shared/ui/confirm-dialog';
import { DistanciaPipe } from '../../../shared/ui/distancia.pipe';
import { ErrorState } from '../../../shared/ui/error-state';
import { LoadingState } from '../../../shared/ui/loading-state';
import { StarRating } from '../../../shared/ui/star-rating';
import { GaleriaImagenes } from './galeria-imagenes';
import { ResenasSection } from './resenas-section';

export const ESTADOS: Record<string, string> = {
  PENDIENTE: 'Pendiente de revisión',
  OCULTO: 'Rechazado por moderación',
  ELIMINADO: 'Eliminado',
};

export const MATERIALES: Record<string, string> = {
  MADERA: 'Madera', METAL: 'Metal', PIEDRA: 'Piedra', HORMIGON: 'Hormigón', PLASTICO: 'Plástico', OTRO: 'Otro',
};

@Component({
  selector: 'bf-banco-detalle-page',
  imports: [
    RouterLink, DatePipe, DecimalPipe, MatButton, MatIcon, MatChipSet, MatChip,
    StarRating, LoadingState, ErrorState, BenchMap, GaleriaImagenes, ResenasSection,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './banco-detalle-page.html',
  styleUrl: './banco-detalle-page.scss',
})
export class BancoDetallePage {
  private readonly bancosApi = inject(BancosApi);
  private readonly usuariosApi = inject(UsuariosApi);
  private readonly geo = inject(GeolocationService);
  private readonly notifier = inject(Notifier);
  private readonly confirmation = inject(Confirmation);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  protected readonly auth = inject(AuthService);

  /** Parámetro de ruta :id. */
  readonly id = input.required<string>();

  private readonly recarga = signal(0);
  protected readonly estados = ESTADOS;
  protected readonly materiales = MATERIALES;

  protected readonly estado = toSignal(
    combineLatest([toObservable(this.id), toObservable(this.recarga)])
      .pipe(switchMap(([id]) => loadable(this.bancosApi.obtener(id)))),
    { initialValue: { status: 'loading' } as Loadable<Banco> },
  );
  protected readonly banco = computed(() => {
    const estado = this.estado();
    return estado.status === 'success' ? estado.value : null;
  });
  protected readonly error = computed(() => {
    const estado = this.estado();
    return estado.status === 'error' ? estado.error : null;
  });
  protected readonly direccion = computed(() => {
    const b = this.banco();
    return b ? [b.direccion, b.ciudad, b.provincia].filter((parte) => !!parte).join(', ') : '';
  });
  protected readonly comoResumen = computed<BancoResumen[]>(() => {
    const b = this.banco();
    return b ? [{
      id: b.id, titulo: b.titulo, lat: b.lat, lng: b.lng, ciudad: b.ciudad, estado: b.estado,
      puntuacionMedia: b.puntuacionMedia, numResenas: b.numResenas, accesible: b.accesible,
      conSombra: b.conSombra, conRespaldo: b.conRespaldo, etiquetas: b.etiquetas.map((e) => e.nombre),
    }] : [];
  });
  protected readonly esGestor = computed(() => {
    const b = this.banco();
    return !!b && (this.auth.isAdmin() || this.auth.session()?.uid === b.autor.id);
  });

  protected readonly favorito = signal(false);
  protected readonly totalCheckIns = signal<number | null>(null);
  protected readonly haciendoCheckIn = signal(false);

  constructor() {
    effect(() => {
      const banco = this.banco();
      if (banco) {
        this.title.setTitle(`${banco.titulo} · Benchify`);
      }
    });
    effect(() => {
      const id = this.id();
      if (this.auth.isAuthenticated()) {
        firstValueFrom(this.usuariosApi.esFavorito(id)).then((f) => this.favorito.set(f), () => undefined);
      } else {
        this.favorito.set(false);
      }
    });
    effect(() => {
      firstValueFrom(this.usuariosApi.totalCheckIns(this.id())).then((t) => this.totalCheckIns.set(t), () => undefined);
    });
  }

  protected recargar(): void {
    this.recarga.update((n) => n + 1);
  }

  protected async alternarFavorito(): Promise<void> {
    if (!this.auth.isAuthenticated()) {
      await this.irALogin();
      return;
    }
    const nuevo = !this.favorito();
    this.favorito.set(nuevo);
    try {
      await firstValueFrom(nuevo ? this.usuariosApi.marcarFavorito(this.id()) : this.usuariosApi.desmarcarFavorito(this.id()));
      this.notifier.success(nuevo ? 'Guardado en favoritos' : 'Quitado de favoritos');
    } catch (error) {
      this.favorito.set(!nuevo);
      this.notifier.error(error);
    }
  }

  protected async checkIn(): Promise<void> {
    if (!this.auth.isAuthenticated()) {
      await this.irALogin();
      return;
    }
    this.haciendoCheckIn.set(true);
    try {
      const posicion = await this.geo.posicionActual();
      const checkIn = await firstValueFrom(this.usuariosApi.checkIn(this.id(), posicion.lat, posicion.lng));
      this.totalCheckIns.update((t) => (t ?? 0) + 1);
      this.notifier.success(`¡Check-in hecho! Estabas a ${new DistanciaPipe().transform(checkIn.distanciaMetros)}.`);
    } catch (error) {
      this.notifier.error(error instanceof Error && !('status' in error) ? error.message : error);
    } finally {
      this.haciendoCheckIn.set(false);
    }
  }

  protected async eliminar(): Promise<void> {
    const ok = await this.confirmation.ask({
      titulo: 'Eliminar banco',
      mensaje: 'Dejará de aparecer en el mapa. Las reseñas y fotos se conservan para moderación.',
      confirmar: 'Eliminar',
      peligroso: true,
    });
    if (!ok) return;
    try {
      await firstValueFrom(this.bancosApi.eliminar(this.id()));
      this.notifier.success('Banco eliminado');
      await this.router.navigateByUrl('/mapa');
    } catch (error) {
      this.notifier.error(error);
    }
  }

  private irALogin(): Promise<boolean> {
    return this.router.navigate(['/login'], { queryParams: { returnUrl: `/bancos/${this.id()}` } });
  }
}
