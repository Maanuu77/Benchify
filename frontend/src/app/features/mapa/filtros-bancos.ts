import { ChangeDetectionStrategy, Component, inject, model } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatChipListbox, MatChipListboxChange, MatChipOption } from '@angular/material/chips';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatOption, MatSelect } from '@angular/material/select';
import { BancosApi } from '../../core/api/bancos-api';
import { FiltroBancos } from '../../core/api/models';

type Caracteristica = 'accesible' | 'conSombra' | 'conRespaldo';

@Component({
  selector: 'bf-filtros-bancos',
  imports: [MatChipListbox, MatChipOption, MatFormField, MatLabel, MatSelect, MatOption],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fieldset class="filtros">
      <legend class="sr-only">Filtrar bancos</legend>
      <mat-chip-listbox multiple aria-label="Características" (change)="cambiarCaracteristicas($event)">
        @for (c of caracteristicas; track c.clave) {
          <mat-chip-option [value]="c.clave" [selected]="!!filtro()[c.clave]">{{ c.etiqueta }}</mat-chip-option>
        }
      </mat-chip-listbox>
      <div class="selects">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Etiqueta</mat-label>
          <mat-select [value]="filtro().etiqueta ?? null" (valueChange)="actualizar({ etiqueta: $event ?? undefined })">
            <mat-option [value]="null">Todas</mat-option>
            @for (etiqueta of etiquetas(); track etiqueta.id) {
              <mat-option [value]="etiqueta.id">{{ etiqueta.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Valoración</mat-label>
          <mat-select [value]="filtro().puntuacionMin ?? null" (valueChange)="actualizar({ puntuacionMin: $event ?? undefined })">
            <mat-option [value]="null">Cualquiera</mat-option>
            <mat-option [value]="3">3 o más</mat-option>
            <mat-option [value]="4">4 o más</mat-option>
            <mat-option [value]="4.5">4,5 o más</mat-option>
          </mat-select>
        </mat-form-field>
      </div>
    </fieldset>
  `,
  styles: `
    .filtros {
      border: 0;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .selects {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
    }
    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
    }
  `,
})
export class FiltrosBancos {
  readonly filtro = model<FiltroBancos>({});

  protected readonly etiquetas = toSignal(inject(BancosApi).etiquetas(), { initialValue: [] });

  protected readonly caracteristicas: { clave: Caracteristica; etiqueta: string }[] = [
    { clave: 'accesible', etiqueta: 'Accesible' },
    { clave: 'conSombra', etiqueta: 'Con sombra' },
    { clave: 'conRespaldo', etiqueta: 'Con respaldo' },
  ];

  protected cambiarCaracteristicas(event: MatChipListboxChange): void {
    const seleccion = new Set<Caracteristica>(event.value as Caracteristica[]);
    this.actualizar({
      accesible: seleccion.has('accesible') || undefined,
      conSombra: seleccion.has('conSombra') || undefined,
      conRespaldo: seleccion.has('conRespaldo') || undefined,
    });
  }

  protected actualizar(cambios: Partial<FiltroBancos>): void {
    this.filtro.update((actual) => ({ ...actual, ...cambios }));
  }
}
