import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { ResenaModeracion } from '../../core/api/models';
import { Notifier } from '../../core/ui/notifier';
import { Confirmation } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { StarRating } from '../../shared/ui/star-rating';
import { Cola } from './cola';

@Component({
  selector: 'bf-cola-resenas',
  imports: [DatePipe, RouterLink, MatButton, MatIcon, StarRating, LoadingState, ErrorState, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>Reseñas reportadas ({{ cola.total() }})</h2>
    @if (cola.error(); as mensaje) {
      <bf-error-state [message]="mensaje" (retry)="cola.cargar()" />
    } @else if (!cola.cargando() && cola.items().length === 0) {
      <bf-empty-state icon="verified" message="No hay reseñas reportadas." />
    } @else {
      <ul class="lista">
        @for (resena of cola.items(); track resena.id) {
          <li class="item">
            <div class="contenido">
              <p class="meta">
                <strong>{{ resena.numReportes }} reportes</strong> ·
                {{ resena.autorNombre ?? resena.autorId }} · {{ resena.fecha | date: 'short' }} ·
                <a [routerLink]="['/bancos', resena.bancoId]">ver banco</a>
              </p>
              <bf-star-rating [value]="resena.puntuacion" />
              <p>{{ resena.comentario ?? '(sin comentario)' }}</p>
            </div>
            <div class="acciones">
              <button mat-stroked-button type="button" [disabled]="ocupado() === resena.id" (click)="mantener(resena)">
                <mat-icon aria-hidden="true">done</mat-icon> Mantener
              </button>
              <button mat-flat-button type="button" [disabled]="ocupado() === resena.id" (click)="eliminar(resena)">
                <mat-icon aria-hidden="true">delete</mat-icon> Borrar
              </button>
            </div>
          </li>
        }
      </ul>
      @if (cola.cargando()) {
        <bf-loading-state />
      } @else if (cola.hayMas()) {
        <button mat-stroked-button type="button" (click)="cola.cargarMas()">Cargar más</button>
      }
    }
  `,
  styleUrl: './cola.scss',
})
export class ColaResenas implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly notifier = inject(Notifier);
  private readonly confirmation = inject(Confirmation);

  protected readonly cola = new Cola<ResenaModeracion>((page) => this.api.resenasReportadas(page));
  protected readonly ocupado = signal<string | null>(null);

  ngOnInit(): void {
    void this.cola.cargar();
  }

  protected async mantener(resena: ResenaModeracion): Promise<void> {
    await this.ejecutar(resena, () => firstValueFrom(this.api.mantenerResena(resena.id)), 'Reseña mantenida');
  }

  protected async eliminar(resena: ResenaModeracion): Promise<void> {
    const ok = await this.confirmation.ask({
      titulo: 'Borrar reseña', mensaje: 'Se borrará y se recalculará la valoración del banco.', confirmar: 'Borrar', peligroso: true,
    });
    if (ok) {
      await this.ejecutar(resena, () => firstValueFrom(this.api.eliminarResena(resena.id)), 'Reseña borrada');
    }
  }

  private async ejecutar(resena: ResenaModeracion, accion: () => Promise<unknown>, mensaje: string): Promise<void> {
    this.ocupado.set(resena.id);
    try {
      await accion();
      this.cola.quitar(resena.id);
      this.notifier.success(mensaje);
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.ocupado.set(null);
    }
  }
}
