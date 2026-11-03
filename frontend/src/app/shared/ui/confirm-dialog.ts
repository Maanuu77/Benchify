import { ChangeDetectionStrategy, Component, inject, Injectable, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogTitle,
} from '@angular/material/dialog';
import { MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';
import { textos } from '../../core/i18n/idioma';
import { CONFIRM_DIALOG } from './confirm-dialog.i18n';

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
      <button mat-button type="button" [mat-dialog-close]="false">{{ t.cancelar }}</button>
      <button mat-flat-button type="button" [class.peligro]="data.peligroso" [mat-dialog-close]="true" cdkFocusInitial>
        {{ data.confirmar ?? t.aceptar }}
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
  protected readonly t = textos(CONFIRM_DIALOG);
}

export interface MotivoData extends ConfirmData {
  /** Motivos frecuentes que se pueden elegir con un toque. */
  sugerencias?: string[];
}

export const MAX_MOTIVO = 500;

/** Confirmación con un motivo opcional, que se guarda en el historial y se envía al autor. */
@Component({
  selector: 'bf-motivo-dialog',
  imports: [
    FormsModule, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose, MatButton,
    MatFormField, MatLabel, MatInput, MatHint,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ data.titulo }}</h2>
    <mat-dialog-content>
      <p class="mensaje">{{ data.mensaje }}</p>
      @if (data.sugerencias?.length) {
        <div class="sugerencias" role="group" [attr.aria-label]="t.motivosFrecuentes">
          @for (s of data.sugerencias; track s) {
            <button type="button" class="sugerencia" [attr.aria-pressed]="motivo() === s" (click)="motivo.set(s)">{{ s }}</button>
          }
        </div>
      }
      <mat-form-field appearance="outline" class="campo">
        <mat-label>{{ t.motivo }}</mat-label>
        <textarea matInput rows="3" [maxlength]="max" [ngModel]="motivo()" (ngModelChange)="motivo.set($event)"></textarea>
        <mat-hint>{{ t.loVeraElAutor }}</mat-hint>
        <mat-hint align="end">{{ motivo().length }}/{{ max }}</mat-hint>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="undefined">{{ t.cancelar }}</button>
      <button mat-flat-button type="button" class="peligro" [mat-dialog-close]="motivo().trim()">
        {{ data.confirmar ?? t.aceptar }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .mensaje {
      margin: 0 0 12px;
    }
    .sugerencias {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-bottom: 12px;
    }
    .sugerencia {
      height: 32px;
      padding: 0 12px;
      border: 1px solid var(--bf-line);
      border-radius: var(--bf-r-pill);
      background: var(--bf-surface-2);
      color: var(--bf-ink);
      font: 500 13px var(--bf-font);
      cursor: pointer;

      &[aria-pressed='true'] {
        background: var(--bf-accent);
        border-color: transparent;
        color: var(--bf-on-accent);
      }
    }
    .campo {
      width: 100%;
    }
    .peligro {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class MotivoDialog {
  protected readonly data = inject<MotivoData>(MAT_DIALOG_DATA);
  protected readonly t = textos(CONFIRM_DIALOG);
  protected readonly max = MAX_MOTIVO;
  protected readonly motivo = signal('');
}

/** Pide confirmación antes de una acción destructiva; resuelve a true si el usuario acepta. */
@Injectable({ providedIn: 'root' })
export class Confirmation {
  private readonly dialog = inject(MatDialog);

  async ask(data: ConfirmData): Promise<boolean> {
    const ref = this.dialog.open(ConfirmDialog, { data, width: '400px', autoFocus: 'dialog' });
    return (await firstValueFrom(ref.afterClosed())) === true;
  }

  /**
   * Pide confirmación con un motivo opcional. Devuelve undefined si se cancela y, si se acepta, el
   * motivo escrito (cadena vacía si no se ha dado ninguno).
   */
  async pedirMotivo(data: MotivoData): Promise<string | undefined> {
    const ref = this.dialog.open(MotivoDialog, { data, width: '480px', autoFocus: 'dialog' });
    const resultado: unknown = await firstValueFrom(ref.afterClosed());
    return typeof resultado === 'string' ? resultado : undefined;
  }
}
