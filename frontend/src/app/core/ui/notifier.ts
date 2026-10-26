import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { toApiError } from '../api/api-error';

/** Avisos breves (snackbar). Los lectores de pantalla los anuncian por la región viva de Material. */
@Injectable({ providedIn: 'root' })
export class Notifier {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'Cerrar', { duration: 4000, politeness: 'polite' });
  }

  error(error: unknown): void {
    const message = typeof error === 'string' ? error : toApiError(error).message;
    this.snackBar.open(message, 'Cerrar', { duration: 6000, politeness: 'assertive', panelClass: 'snack-error' });
  }
}
