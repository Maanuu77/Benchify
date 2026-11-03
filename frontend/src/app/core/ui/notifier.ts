import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { toApiError } from '../api/api-error';
import { CORE } from '../i18n/core.i18n';
import { textos } from '../i18n/idioma';

/** Avisos breves (snackbar). Los lectores de pantalla los anuncian por la región viva de Material. */
@Injectable({ providedIn: 'root' })
export class Notifier {
  private readonly snackBar = inject(MatSnackBar);
  private readonly t = textos(CORE);

  success(message: string): void {
    this.snackBar.open(message, this.t.cerrar, { duration: 4000, politeness: 'polite' });
  }

  error(error: unknown): void {
    const message = typeof error === 'string' ? error : toApiError(error).message;
    this.snackBar.open(message, this.t.cerrar, { duration: 6000, politeness: 'assertive', panelClass: 'snack-error' });
  }
}
