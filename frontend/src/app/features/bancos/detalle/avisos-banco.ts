import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
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
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { Router } from '@angular/router';
import { catchError, combineLatest, firstValueFrom, of, switchMap } from 'rxjs';
import { IncidenciasApi } from '../../../core/api/incidencias-api';
import { TipoIncidencia } from '../../../core/api/models';
import { AuthService } from '../../../core/auth/auth.service';
import { textos, textosActuales } from '../../../core/i18n/idioma';
import { Notifier } from '../../../core/ui/notifier';
import { AVISOS_BANCO } from './avisos-banco.i18n';

const ICONOS_INCIDENCIA: Record<TipoIncidencia, string> = {
  ROTO: 'construction',
  SUCIO: 'cleaning_services',
  DESAPARECIDO: 'location_off',
  OTRO: 'report',
};

/** Etiqueta en el idioma indicado e icono de cada tipo de incidencia. */
function tiposIncidencia(
  etiquetas: Record<TipoIncidencia, string>,
): Record<TipoIncidencia, { etiqueta: string; icono: string }> {
  const tipos = Object.keys(ICONOS_INCIDENCIA) as TipoIncidencia[];
  return Object.fromEntries(
    tipos.map((tipo) => [tipo, { etiqueta: etiquetas[tipo], icono: ICONOS_INCIDENCIA[tipo] }]),
  ) as Record<TipoIncidencia, { etiqueta: string; icono: string }>;
}

export const TIPOS_INCIDENCIA = tiposIncidencia(textosActuales(AVISOS_BANCO).tipos);

export const MAX_COMENTARIO_INCIDENCIA = 500;

export interface AvisoIncidencia {
  tipo: TipoIncidencia;
  comentario: string;
}

/** Formulario del aviso: un tipo y, si es "otro", una explicación. */
@Component({
  selector: 'bf-incidencia-dialog',
  imports: [
    FormsModule, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose, MatButton, MatIcon,
    MatFormField, MatLabel, MatInput, MatHint,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ t.titulo }}</h2>
    <mat-dialog-content>
      <p class="mensaje">{{ t.queLePasa(titulo) }}</p>
      <div class="tipos" role="radiogroup" [attr.aria-label]="t.tipoDeProblema">
        @for (opcion of tipos; track opcion) {
          <button type="button" class="tipo" role="radio" [attr.aria-checked]="tipo() === opcion" (click)="tipo.set(opcion)">
            <mat-icon aria-hidden="true">{{ info[opcion].icono }}</mat-icon>{{ info[opcion].etiqueta }}
          </button>
        }
      </div>
      <mat-form-field appearance="outline" class="campo">
        <mat-label>{{ tipo() === 'OTRO' ? t.cuentanos : t.detalles }}</mat-label>
        <textarea matInput rows="3" [maxlength]="max" [ngModel]="comentario()" (ngModelChange)="comentario.set($event)"></textarea>
        <mat-hint align="end">{{ comentario().length }}/{{ max }}</mat-hint>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="undefined">{{ t.cancelar }}</button>
      <button mat-flat-button type="button" [disabled]="!valido()" [mat-dialog-close]="aviso()">{{ t.enviar }}</button>
    </mat-dialog-actions>
  `,
  styles: `
    .mensaje {
      margin: 0 0 12px;
    }
    .tipos {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 8px;
      margin-bottom: 16px;
    }
    .tipo {
      display: flex;
      align-items: center;
      gap: 8px;
      min-height: 48px;
      padding: 0 12px;
      border: 1px solid var(--bf-line);
      border-radius: 14px;
      background: var(--bf-surface-2);
      color: var(--bf-ink);
      font: 600 14px var(--bf-font);
      text-align: left;
      cursor: pointer;

      mat-icon {
        color: var(--bf-accent-ink);
      }
      &[aria-checked='true'] {
        background: var(--bf-accent);
        border-color: transparent;
        color: var(--bf-on-accent);

        mat-icon {
          color: inherit;
        }
      }
    }
    .campo {
      width: 100%;
    }
  `,
})
export class IncidenciaDialog {
  protected readonly titulo = inject<string>(MAT_DIALOG_DATA);
  protected readonly t = textos(AVISOS_BANCO);
  protected readonly info = tiposIncidencia(this.t.tipos);
  protected readonly tipos = Object.keys(TIPOS_INCIDENCIA) as TipoIncidencia[];
  protected readonly max = MAX_COMENTARIO_INCIDENCIA;
  protected readonly tipo = signal<TipoIncidencia | null>(null);
  protected readonly comentario = signal('');
  protected readonly valido = computed(() => {
    const tipo = this.tipo();
    return !!tipo && (tipo !== 'OTRO' || this.comentario().trim().length > 0);
  });
  protected readonly aviso = computed<AvisoIncidencia | undefined>(() => {
    const tipo = this.tipo();
    return tipo ? { tipo, comentario: this.comentario().trim() } : undefined;
  });
}

/** Avisos abiertos de la comunidad sobre el banco y botón para añadir uno. */
@Component({
  selector: 'bf-avisos-banco',
  imports: [MatIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (resumen().length) {
      <div class="aviso" role="status">
        <mat-icon aria-hidden="true">warning_amber</mat-icon>
        <p>
          <strong>{{ t.comunidadAvisa }}</strong>
          @for (r of resumen(); track r.tipo; let ultimo = $last) {
            {{ info[r.tipo].etiqueta.toLowerCase() }} ({{ r.total }}){{ ultimo ? '' : ' · ' }}
          }
        </p>
      </div>
    }
    @if (publicado()) {
      <button type="button" class="avisar" [disabled]="enviando()" (click)="avisar()">
        <mat-icon aria-hidden="true">flag</mat-icon>
        {{ t.avisanos }}
      </button>
    }
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .aviso {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 12px 14px;
      border-radius: var(--bf-r);
      background: rgb(245 158 11 / 14%);
      color: var(--bf-ink);
      font-size: 14px;

      mat-icon {
        color: light-dark(#b45309, #fbbf24);
      }
      p {
        margin: 2px 0 0;
      }
    }
    .avisar {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      align-self: flex-start;
      padding: 4px 0;
      border: 0;
      background: none;
      color: var(--bf-ink-2);
      font: 500 13px var(--bf-font);
      cursor: pointer;

      mat-icon {
        width: 18px;
        height: 18px;
        font-size: 18px;
      }
      &:hover {
        color: var(--bf-ink);
        text-decoration: underline;
      }
    }
  `,
})
export class AvisosBanco {
  private readonly api = inject(IncidenciasApi);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly notifier = inject(Notifier);
  private readonly router = inject(Router);

  readonly bancoId = input.required<string>();
  readonly tituloBanco = input.required<string>();
  /** Solo se puede avisar de bancos publicados. */
  readonly publicado = input(true);

  protected readonly t = textos(AVISOS_BANCO);
  protected readonly info = tiposIncidencia(this.t.tipos);
  protected readonly enviando = signal(false);
  private readonly recarga = signal(0);

  protected readonly resumen = toSignal(
    combineLatest([toObservable(this.bancoId), toObservable(this.recarga)]).pipe(
      switchMap(([id]) => this.api.resumen(id).pipe(catchError(() => of([])))),
    ),
    { initialValue: [] },
  );

  protected async avisar(): Promise<void> {
    if (!this.auth.isAuthenticated()) {
      await this.router.navigate(['/login'], { queryParams: { returnUrl: `/bancos/${this.bancoId()}` } });
      return;
    }
    const ref = this.dialog.open(IncidenciaDialog, { data: this.tituloBanco(), width: '480px', autoFocus: 'dialog' });
    const aviso: AvisoIncidencia | undefined = await firstValueFrom(ref.afterClosed());
    if (!aviso) return;
    this.enviando.set(true);
    try {
      await firstValueFrom(this.api.reportar(this.bancoId(), aviso.tipo, aviso.comentario));
      this.notifier.success(this.t.gracias);
      this.recarga.update((n) => n + 1);
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.enviando.set(false);
    }
  }
}
