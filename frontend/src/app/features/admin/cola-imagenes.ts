import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { ImagenModeracion } from '../../core/api/models';
import { Notifier } from '../../core/ui/notifier';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { Cola } from './cola';

@Component({
  selector: 'bf-cola-imagenes',
  imports: [DatePipe, RouterLink, MatButton, LoadingState, ErrorState, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>Fotos pendientes ({{ cola.total() }})</h2>
    @if (cola.error(); as mensaje) {
      <bf-error-state [message]="mensaje" (retry)="cola.cargar()" />
    } @else if (!cola.cargando() && cola.items().length === 0) {
      <bf-empty-state icon="photo_library" message="No hay fotos pendientes." />
    } @else {
      <ul class="galeria">
        @for (imagen of cola.items(); track imagen.id; let i = $index) {
          <li>
            <img [src]="imagen.url" [alt]="'Foto pendiente ' + (i + 1)" loading="lazy" />
            <p class="meta">
              {{ imagen.fechaSubida | date: 'short' }} · {{ tamano(imagen.tamanoBytes) }} ·
              <a [routerLink]="['/bancos', imagen.bancoId]">ver banco</a>
            </p>
            <div class="acciones">
              <button mat-flat-button type="button" [disabled]="ocupado() === imagen.id" (click)="aprobar(imagen)">Aprobar</button>
              <button mat-stroked-button type="button" [disabled]="ocupado() === imagen.id" (click)="rechazar(imagen)">Rechazar</button>
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
export class ColaImagenes implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly notifier = inject(Notifier);

  protected readonly cola = new Cola<ImagenModeracion>((page) => this.api.imagenesPendientes(page), 24);
  protected readonly ocupado = signal<string | null>(null);

  ngOnInit(): void {
    void this.cola.cargar();
  }

  protected tamano(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
    return `${(bytes / (1024 * 1024)).toLocaleString('es-ES', { maximumFractionDigits: 1 })} MB`;
  }

  protected aprobar(imagen: ImagenModeracion): Promise<void> {
    return this.ejecutar(imagen, () => firstValueFrom(this.api.aprobarImagen(imagen.id)), 'Foto aprobada');
  }

  protected rechazar(imagen: ImagenModeracion): Promise<void> {
    return this.ejecutar(imagen, () => firstValueFrom(this.api.rechazarImagen(imagen.id)), 'Foto rechazada y borrada');
  }

  private async ejecutar(imagen: ImagenModeracion, accion: () => Promise<unknown>, mensaje: string): Promise<void> {
    this.ocupado.set(imagen.id);
    try {
      await accion();
      this.cola.quitar(imagen.id);
      this.notifier.success(mensaje);
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.ocupado.set(null);
    }
  }
}
