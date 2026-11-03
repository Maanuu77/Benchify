import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, LOCALE_ID, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { ImagenModeracion, ImagenParecida } from '../../core/api/models';
import { textos } from '../../core/i18n/idioma';
import { Notifier } from '../../core/ui/notifier';
import { ImagenSrc } from '../../shared/imagenes/imagen-src';
import { Confirmation } from '../../shared/ui/confirm-dialog';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { COLA } from './admin.i18n';
import { Cola } from './cola';
import { COLA_IMAGENES } from './cola-imagenes.i18n';

@Component({
  selector: 'bf-cola-imagenes',
  imports: [DatePipe, RouterLink, MatButton, MatIcon, LoadingState, ErrorState, EmptyState, ImagenSrc],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>{{ t.titulo }} ({{ cola.total() }})</h2>
    @if (cola.error(); as mensaje) {
      <bf-error-state [message]="mensaje" (retry)="cola.cargar()" />
    } @else if (!cola.cargando() && cola.items().length === 0) {
      <bf-empty-state icon="photo_library" [message]="t.vacia" />
    } @else {
      <ul class="galeria">
        @for (imagen of cola.items(); track imagen.id; let i = $index) {
          <li>
            <img [bfImagenSrc]="imagen.thumbUrl" [autenticada]="imagen.requiereAutenticacion"
                 [alt]="t.fotoPendiente(i + 1)" loading="lazy" />
            <p class="meta">
              {{ imagen.fechaSubida | date: 'short' }} · {{ tamano(imagen.tamanoBytes) }} ·
              <a [routerLink]="['/bancos', imagen.bancoId]">{{ tc.verBanco }}</a>
            </p>
            @if (imagen.parecidas.length) {
              <div class="parecidas" role="note">
                <p><mat-icon aria-hidden="true">content_copy</mat-icon> {{ t.seParece(imagen.parecidas.length) }}</p>
                <ul>
                  @for (p of imagen.parecidas; track p.id) {
                    <li>
                      <a [routerLink]="['/bancos', p.bancoId]" [attr.aria-label]="descripcion(p)" [title]="descripcion(p)">
                        @if (p.thumbUrl) {
                          <img [bfImagenSrc]="p.thumbUrl" [autenticada]="p.requiereAutenticacion" alt="" loading="lazy" />
                        } @else {
                          <span class="sin-foto"><mat-icon aria-hidden="true">hide_image</mat-icon></span>
                        }
                        <span class="etiqueta">{{ p.mismoBanco ? t.esteBanco : t.otroBanco }} · {{ t.estados[p.estado] }}</span>
                      </a>
                    </li>
                  }
                </ul>
              </div>
            }
            <div class="acciones">
              <button mat-flat-button type="button" [disabled]="ocupado() === imagen.id" (click)="aprobar(imagen)">{{ t.aprobar }}</button>
              <button mat-stroked-button type="button" [disabled]="ocupado() === imagen.id" (click)="rechazar(imagen)">{{ t.rechazar }}</button>
            </div>
          </li>
        }
      </ul>
      @if (cola.cargando()) {
        <bf-loading-state />
      } @else if (cola.hayMas()) {
        <button mat-stroked-button type="button" (click)="cola.cargarMas()">{{ tc.cargarMas }}</button>
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
  private readonly locale = inject(LOCALE_ID);
  protected readonly t = textos(COLA_IMAGENES);
  protected readonly tc = textos(COLA);

  protected readonly cola = new Cola<ImagenModeracion>((page) => this.api.imagenesPendientes(page), 24);
  protected readonly ocupado = signal<string | null>(null);

  ngOnInit(): void {
    void this.cola.cargar();
  }

  protected descripcion(parecida: ImagenParecida): string {
    return this.t.parecida(parecida.mismoBanco, this.t.estados[parecida.estado]);
  }

  protected tamano(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
    return `${(bytes / (1024 * 1024)).toLocaleString(this.locale, { maximumFractionDigits: 1 })} MB`;
  }

  protected aprobar(imagen: ImagenModeracion): Promise<void> {
    return this.ejecutar(imagen, () => firstValueFrom(this.api.aprobarImagen(imagen.id)), this.t.aprobada);
  }

  protected async rechazar(imagen: ImagenModeracion): Promise<void> {
    const motivo = await this.confirmation.pedirMotivo({
      titulo: this.t.rechazarFoto,
      mensaje: this.t.rechazarMensaje,
      confirmar: this.t.rechazar,
      sugerencias: this.t.sugerencias,
    });
    if (motivo === undefined) return;
    await this.ejecutar(imagen, () => firstValueFrom(this.api.rechazarImagen(imagen.id, motivo)), this.t.rechazada);
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
