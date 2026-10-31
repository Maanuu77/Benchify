import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, firstValueFrom, of, switchMap } from 'rxjs';
import { toApiError } from '../../../core/api/api-error';
import { Resena } from '../../../core/api/models';
import { ResenasApi } from '../../../core/api/resenas-api';
import { AuthService } from '../../../core/auth/auth.service';
import { Notifier } from '../../../core/ui/notifier';
import { Confirmation } from '../../../shared/ui/confirm-dialog';
import { EmptyState } from '../../../shared/ui/empty-state';
import { ErrorState } from '../../../shared/ui/error-state';
import { LoadingState } from '../../../shared/ui/loading-state';
import { StarRating } from '../../../shared/ui/star-rating';

const TAMANO_PAGINA = 20;

@Component({
  selector: 'bf-resenas-section',
  imports: [
    FormsModule, DatePipe, RouterLink, StarRating, LoadingState, ErrorState, EmptyState,
    MatButton, MatIcon, MatFormField, MatLabel, MatInput, MatHint,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './resenas-section.html',
  styleUrl: './resenas-section.scss',
})
export class ResenasSection {
  private readonly api = inject(ResenasApi);
  private readonly notifier = inject(Notifier);
  private readonly confirmation = inject(Confirmation);
  protected readonly auth = inject(AuthService);

  readonly bancoId = input.required<string>();
  readonly autorBancoId = input.required<string>();
  readonly publicado = input(true);
  /** Avisa a la ficha para que recargue la media de puntuación. */
  readonly valoracionCambiada = output<void>();

  protected readonly resenas = signal<Resena[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly editando = signal<Resena | null>(null);
  protected puntuacion = 0;
  protected comentario = '';
  protected readonly enviando = signal(false);
  /** Se intentó enviar sin puntuación: el error se muestra junto a las estrellas. */
  protected readonly faltaPuntuacion = signal(false);

  protected readonly miUid = computed(() => this.auth.session()?.uid ?? null);
  private readonly recargaMia = signal(0);
  /**
   * La reseña propia se pide aparte: puede estar en una página que no se ha cargado. Si falla la
   * petición se busca en las cargadas, que es mejor que ofrecer un formulario que dará duplicado.
   */
  protected readonly miResena = toSignal(
    combineLatest([toObservable(this.bancoId), toObservable(this.miUid), toObservable(this.recargaMia)]).pipe(
      switchMap(([bancoId, uid]) =>
        uid
          ? this.api.mia(bancoId).pipe(catchError(() => of(this.resenas().find((r) => r.autorId === uid) ?? null)))
          : of(null),
      ),
    ),
    { initialValue: null },
  );
  protected readonly puedeEscribir = computed(() =>
    this.auth.isAuthenticated() && this.publicado() && this.miUid() !== this.autorBancoId()
    && (!this.miResena() || this.editando() !== null));
  protected readonly hayMas = computed(() => this.resenas().length < this.total());

  constructor() {
    effect(() => {
      this.bancoId();
      void this.recargar();
    });
  }

  async recargar(): Promise<void> {
    this.pagina.set(0);
    this.resenas.set([]);
    await this.cargarPagina(0);
  }

  protected async cargarMas(): Promise<void> {
    await this.cargarPagina(this.pagina() + 1);
  }

  protected empezarEdicion(resena: Resena): void {
    this.editando.set(resena);
    this.puntuacion = resena.puntuacion;
    this.comentario = resena.comentario ?? '';
  }

  protected cancelarEdicion(): void {
    this.editando.set(null);
    this.limpiarFormulario();
  }

  protected async enviar(): Promise<void> {
    if (this.puntuacion < 1) {
      this.faltaPuntuacion.set(true);
      return;
    }
    this.enviando.set(true);
    const request = { puntuacion: this.puntuacion, comentario: this.comentario.trim() || null };
    try {
      const editando = this.editando();
      if (editando) {
        await firstValueFrom(this.api.editar(editando.id, request));
        this.notifier.success('Reseña actualizada');
      } else {
        await firstValueFrom(this.api.publicar(this.bancoId(), request));
        this.notifier.success('Gracias por tu reseña');
      }
      this.editando.set(null);
      this.limpiarFormulario();
      this.recargaMia.update((n) => n + 1);
      await this.recargar();
      this.valoracionCambiada.emit();
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.enviando.set(false);
    }
  }

  protected async borrar(resena: Resena): Promise<void> {
    const ok = await this.confirmation.ask({
      titulo: 'Borrar reseña', mensaje: '¿Seguro que quieres borrar tu reseña?', confirmar: 'Borrar', peligroso: true,
    });
    if (!ok) return;
    try {
      await firstValueFrom(this.api.eliminar(resena.id));
      this.notifier.success('Reseña borrada');
      this.recargaMia.update((n) => n + 1);
      await this.recargar();
      this.valoracionCambiada.emit();
    } catch (error) {
      this.notifier.error(error);
    }
  }

  protected async reportar(resena: Resena): Promise<void> {
    const ok = await this.confirmation.ask({
      titulo: 'Reportar reseña',
      mensaje: 'La revisará un moderador si varios usuarios la reportan. ¿Es ofensiva, spam o no habla del banco?',
      confirmar: 'Reportar',
    });
    if (!ok) return;
    try {
      await firstValueFrom(this.api.reportar(resena.id));
      this.notifier.success('Gracias, hemos recibido tu reporte');
    } catch (error) {
      this.notifier.error(error);
    }
  }

  private async cargarPagina(pagina: number): Promise<void> {
    this.cargando.set(true);
    this.error.set(null);
    try {
      const page = await firstValueFrom(this.api.deBanco(this.bancoId(), { page: pagina, size: TAMANO_PAGINA }));
      this.resenas.update((actuales) => (pagina === 0 ? page.content : [...actuales, ...page.content]));
      this.total.set(page.totalElements);
      this.pagina.set(pagina);
    } catch (error) {
      this.error.set(toApiError(error).message);
    } finally {
      this.cargando.set(false);
    }
  }

  private limpiarFormulario(): void {
    this.faltaPuntuacion.set(false);
    this.puntuacion = 0;
    this.comentario = '';
  }
}
