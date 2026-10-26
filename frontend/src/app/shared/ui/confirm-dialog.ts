import { ChangeDetectionStrategy, Component, inject, Injectable } from '@angular/core';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogTitle,
} from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';

export interface ConfirmData {
  titulo: string;
  mensaje: string;
  confirmar?: string;
  peligroso?: boolean;
}

@Component({
  selector: 'bf-confirm-dialog',
  imports: [MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose, MatButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ data.titulo }}</h2>
    <mat-dialog-content>{{ data.mensaje }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button type="button" [class.peligro]="data.peligroso" [mat-dialog-close]="true" cdkFocusInitial>
        {{ data.confirmar ?? 'Aceptar' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .peligro {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class ConfirmDialog {
  protected readonly data = inject<ConfirmData>(MAT_DIALOG_DATA);
}

/** Pide confirmación antes de una acción destructiva; resuelve a true si el usuario acepta. */
@Injectable({ providedIn: 'root' })
export class Confirmation {
  private readonly dialog = inject(MatDialog);

  async ask(data: ConfirmData): Promise<boolean> {
    const ref = this.dialog.open(ConfirmDialog, { data, width: '400px', autoFocus: 'dialog' });
    return (await firstValueFrom(ref.afterClosed())) === true;
  }
}
