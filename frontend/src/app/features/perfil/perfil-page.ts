import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatListItem, MatListItemLine, MatListItemTitle, MatNavList } from '@angular/material/list';
import { MatTab, MatTabGroup } from '@angular/material/tabs';
import { RouterLink } from '@angular/router';
import { filter, firstValueFrom, forkJoin, map, switchMap } from 'rxjs';
import { Loadable, loadable } from '../../core/api/api-error';
import { BancosApi } from '../../core/api/bancos-api';
import { BancoResumen, CheckIn, EstadisticasUsuario, Favorito, Page, PerfilPublico, Resena } from '../../core/api/models';
import { ResenasApi } from '../../core/api/resenas-api';
import { UsuariosApi } from '../../core/api/usuarios-api';
import { AuthService } from '../../core/auth/auth.service';
import { Notifier } from '../../core/ui/notifier';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { StarRating } from '../../shared/ui/star-rating';

interface DatosPerfil {
  perfil: PerfilPublico;
  estadisticas: EstadisticasUsuario;
  bancos: Page<BancoResumen>;
  resenas: Page<Resena>;
}

@Component({
  selector: 'bf-perfil-page',
  imports: [
    RouterLink, DatePipe, DecimalPipe, FormsModule, StarRating, LoadingState, ErrorState, EmptyState,
    MatButton, MatIcon, MatTabGroup, MatTab, MatNavList, MatListItem, MatListItemTitle, MatListItemLine,
    MatFormField, MatLabel, MatInput,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './perfil-page.html',
  styleUrl: './perfil-page.scss',
})
export class PerfilPage {
  private readonly usuariosApi = inject(UsuariosApi);
  private readonly bancosApi = inject(BancosApi);
  private readonly resenasApi = inject(ResenasApi);
  private readonly notifier = inject(Notifier);
  private readonly auth = inject(AuthService);

  /** :id en /usuarios/:id; sin él se muestra el perfil propio (/perfil). */
  readonly id = input<string>();

  protected readonly uid = computed(() => this.id() ?? this.auth.session()?.uid ?? null);
  protected readonly esPropio = computed(() => !!this.uid() && this.uid() === this.auth.session()?.uid);
  private readonly recarga = signal(0);

  protected readonly estado = toSignal(
    toObservable(computed(() => ({ uid: this.uid(), recarga: this.recarga() }))).pipe(
      map(({ uid }) => uid),
      filter((uid): uid is string => uid !== null),
      switchMap((uid) => loadable(forkJoin({
        perfil: this.usuariosApi.perfil(uid),
        estadisticas: this.usuariosApi.estadisticas(uid),
        bancos: this.bancosApi.deUsuario(uid, { size: 50 }),
        resenas: this.resenasApi.deUsuario(uid, { size: 50 }),
      }))),
    ),
    { initialValue: { status: 'loading' } as Loadable<DatosPerfil> },
  );

  protected readonly datosPerfil = computed(() => {
    const estado = this.estado();
    return estado.status === 'success' ? estado.value : null;
  });

  protected readonly favoritos = signal<Favorito[] | null>(null);
  protected readonly checkIns = signal<CheckIn[] | null>(null);

  protected readonly editando = signal(false);
  protected nombre = '';
  protected ciudad = '';

  constructor() {
    effect(() => {
      if (this.esPropio()) {
        firstValueFrom(this.usuariosApi.misFavoritos({ size: 50 })).then((p) => this.favoritos.set(p.content), () => this.favoritos.set([]));
        firstValueFrom(this.usuariosApi.misCheckIns({ size: 50 })).then((p) => this.checkIns.set(p.content), () => this.checkIns.set([]));
      }
    });
  }

  protected reintentar(): void {
    this.recarga.update((n) => n + 1);
  }

  protected editar(perfil: PerfilPublico): void {
    this.nombre = perfil.nombre;
    this.ciudad = perfil.ciudad ?? '';
    this.editando.set(true);
  }

  protected async guardar(): Promise<void> {
    if (!this.nombre.trim()) {
      this.notifier.error('El nombre es obligatorio.');
      return;
    }
    try {
      await firstValueFrom(this.usuariosApi.actualizarPerfil(this.nombre.trim(), this.ciudad.trim() || null));
      this.editando.set(false);
      this.notifier.success('Perfil actualizado');
      this.reintentar();
    } catch (error) {
      this.notifier.error(error);
    }
  }
}
