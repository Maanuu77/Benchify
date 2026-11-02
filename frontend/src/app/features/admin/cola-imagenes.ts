import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { EstadoImagen, ImagenModeracion, ImagenParecida } from '../../core/api/models';
import { Notifier } from '../../core/ui/notifier';
import { ImagenSrc } from '../../shared/imagenes/imagen-src';
import { Confirmation } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { Cola } from './cola';

@Component({
  selector: 'bf-cola-imagenes',
  imports: [DatePipe, RouterLink, MatButton, MatIcon, LoadingState, ErrorState, EmptyState, ImagenSrc],
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
            <img [bfImagenSrc]="imagen.thumbUrl" [autenticada]="imagen.requiereAutenticacion"
                 [alt]="'Foto pendiente ' + (i + 1)" loading="lazy" />
            <p class="meta">
              {{ imagen.fechaSubida | date: 'short' }} · {{ tamano(imagen.tamanoBytes) }} ·
              <a [routerLink]="['/bancos', imagen.bancoId]">ver banco</a>
            </p>
            @if (imagen.parecidas.length) {
              <div class="parecidas" role="note">
                <p><mat-icon aria-hidden="true">content_copy</mat-icon> Se parece a {{ imagen.parecidas.length === 1 ? 'otra foto' : imagen.parecidas.length + ' fotos' }}</p>
                <ul>
                  @for (p of imagen.parecidas; track p.id) {
                    <li>
                      <a [routerLink]="['/bancos', p.bancoId]" [attr.aria-label]="descripcion(p)" [title]="descripcion(p)">
                        @if (p.thumbUrl) {
                          <img [bfImagenSrc]="p.thumbUrl" [autenticada]="p.requiereAutenticacion" alt="" loading="lazy" />
                        } @else {
                          <span class="sin-foto"><mat-icon aria-hidden="true">hide_image</mat-icon></span>
                        }
                        <span class="etiqueta">{{ p.mismoBanco ? 'Este banco' : 'Otro banco' }} · {{ estados[p.estado] }}</span>
                      </a>
                    </li>
                  }
                </ul>
              </div>
            }
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
  styles: `
    .galeria .parecidas {
      margin: 0 4px 10px;
      padding: 8px;
      border-radius: var(--bf-r-sm);
      background: rgb(245 158 11 / 14%);

      p {
        display: flex;
        align-items: center;
        gap: 6px;
        margin: 0 0 6px;
        font: 600 13px var(--bf-font);
      }
      p mat-icon {
        width: 18px;
        height: 18px;
        font-size: 18px;
        color: light-dark(#b45309, #fbbf24);
      }
      ul {
        display: flex;
        gap: 6px;
        margin: 0;
        padding: 0;
        list-style: none;
      }
      li {
        padding: 0;
        border: 0;
        background: none;
      }
      a {
        display: flex;
        flex-direction: column;
        gap: 2px;
        width: 72px;
        color: var(--bf-ink-2);
        text-decoration: none;
      }
      img,
      .sin-foto {
        width: 72px;
        height: 54px;
        aspect-ratio: auto;
        border-radius: 8px;
      }
      .sin-foto {
        display: grid;
        place-items: center;
        background: var(--bf-surface-2);
      }
      .etiqueta {
        font-size: 10px;
        line-height: 1.2;
      }
    }
  `,
})
export class ColaImagenes implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly notifier = inject(Notifier);
  private readonly confirmation = inject(Confirmation);

  protected readonly cola = new Cola<ImagenModeracion>((page) => this.api.imagenesPendientes(page), 24);
  protected readonly ocupado = signal<string | null>(null);

  ngOnInit(): void {
    void this.cola.cargar();
  }

  protected readonly estados: Record<EstadoImagen, string> = {
    APROBADA: 'publicada', PENDIENTE: 'pendiente', RECHAZADA: 'rechazada',
  };

  protected descripcion(parecida: ImagenParecida): string {
    return `Foto parecida (${parecida.mismoBanco ? 'de este banco' : 'de otro banco'}, ${this.estados[parecida.estado]})`;
  }

  protected tamano(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
    return `${(bytes / (1024 * 1024)).toLocaleString('es-ES', { maximumFractionDigits: 1 })} MB`;
  }

  protected aprobar(imagen: ImagenModeracion): Promise<void> {
    return this.ejecutar(imagen, () => firstValueFrom(this.api.aprobarImagen(imagen.id)), 'Foto aprobada');
  }

  protected async rechazar(imagen: ImagenModeracion): Promise<void> {
    const motivo = await this.confirmation.pedirMotivo({
      titulo: 'Rechazar foto',
      mensaje: 'La foto se borrará. Puedes explicar el motivo a quien la subió.',
      confirmar: 'Rechazar',
      sugerencias: ['No se ve el banco', 'Foto borrosa u oscura', 'Aparecen personas', 'Contenido inapropiado'],
    });
    if (motivo === undefined) return;
    await this.ejecutar(imagen, () => firstValueFrom(this.api.rechazarImagen(imagen.id, motivo)), 'Foto rechazada y borrada');
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
