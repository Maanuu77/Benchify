import { DestroyRef, inject, Injectable } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SwUpdate, VersionReadyEvent } from '@angular/service-worker';
import { filter, interval } from 'rxjs';
import { CORE } from '../i18n/core.i18n';
import { textos } from '../i18n/idioma';

/** Cada cuánto se busca una versión nueva si la aplicación se queda abierta. */
export const INTERVALO_COMPROBACION_MS = 6 * 60 * 60 * 1000;

/**
 * Avisa cuando el service worker ya tiene descargada una versión nueva: se aplica al recargar, y
 * es el usuario quien decide cuándo (podría estar a mitad de un formulario).
 */
@Injectable({ providedIn: 'root' })
export class Actualizaciones {
  private readonly sw = inject(SwUpdate);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly t = textos(CORE);

  iniciar(): void {
    if (!this.sw.isEnabled) {
      return;
    }
    this.sw.versionUpdates
      .pipe(filter((e): e is VersionReadyEvent => e.type === 'VERSION_READY'), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.ofrecerRecarga(this.t.versionNueva, this.t.actualizar));
    // La versión en caché ya no se puede servir entera (p. ej. el navegador borró parte): hay que recargar
    this.sw.unrecoverable
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.ofrecerRecarga(this.t.hayQueRecargar, this.t.recargar));
    interval(INTERVALO_COMPROBACION_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => void this.sw.checkForUpdate().catch(() => undefined));
  }

  private ofrecerRecarga(mensaje: string, accion: string): void {
    this.snackBar.open(mensaje, accion, { politeness: 'polite' })
      .onAction()
      .subscribe(() => document.location.reload());
  }
}
