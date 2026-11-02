import { DOCUMENT } from '@angular/common';
import { computed, inject, Injectable, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, EMPTY, filter, firstValueFrom, fromEvent, map, merge, of, startWith, switchMap, timer } from 'rxjs';
import { Notificacion } from '../api/models';
import { NotificacionesApi } from '../api/notificaciones-api';
import { AuthService } from '../auth/auth.service';

/** Cada cuánto se vuelve a preguntar por avisos nuevos mientras la pestaña está visible. */
export const INTERVALO_NOTIFICACIONES_MS = 60_000;

/**
 * Contador de avisos sin leer para la campana de la cabecera. Se actualiza al iniciar sesión, cada
 * minuto, al volver a la pestaña y después de marcar avisos como leídos.
 */
@Injectable({ providedIn: 'root' })
export class NotificacionesService {
  private readonly api = inject(NotificacionesApi);
  private readonly auth = inject(AuthService);
  private readonly document = inject(DOCUMENT);
  private readonly refresco = signal(0);

  private readonly uid = computed(() => this.auth.session()?.uid ?? null);

  readonly noLeidas = toSignal(
    combineLatest([
      toObservable(this.uid),
      toObservable(this.refresco),
      merge(
        timer(INTERVALO_NOTIFICACIONES_MS, INTERVALO_NOTIFICACIONES_MS),
        fromEvent(this.document, 'visibilitychange').pipe(filter(() => !this.document.hidden)),
      ).pipe(startWith(null)),
    ]).pipe(
      // Con la pestaña oculta no se pregunta y se conserva el último valor
      switchMap(([uid]) => {
        if (!uid) return of(0);
        if (this.document.hidden) return EMPTY;
        return this.api.noLeidas().pipe(catchError(() => EMPTY));
      }),
    ),
    { initialValue: 0 },
  );

  /** Los avisos más recientes, para el menú de la campana. */
  async recientes(): Promise<Notificacion[]> {
    return firstValueFrom(this.api.mias({ size: 10 }).pipe(map((page) => page.content)));
  }

  async marcarLeida(id: string): Promise<void> {
    await firstValueFrom(this.api.marcarLeida(id));
    this.refrescar();
  }

  async marcarTodasLeidas(): Promise<void> {
    await firstValueFrom(this.api.marcarTodasLeidas());
    this.refrescar();
  }

  refrescar(): void {
    this.refresco.update((n) => n + 1);
  }
}
