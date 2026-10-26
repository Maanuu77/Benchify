import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AdminApi } from '../../core/api/admin-api';
import { BancoModeracion } from '../../core/api/models';
import { Notifier } from '../../core/ui/notifier';
import { EmptyState } from '../../shared/ui/empty-state';
import { ErrorState } from '../../shared/ui/error-state';
import { LoadingState } from '../../shared/ui/loading-state';
import { Cola } from './cola';

@Component({
  selector: 'bf-cola-bancos',
  imports: [DatePipe, RouterLink, MatButton, MatIcon, LoadingState, ErrorState, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>Bancos pendientes ({{ cola.total() }})</h2>
    @if (cola.error(); as mensaje) {
      <bf-error-state [message]="mensaje" (retry)="cola.cargar()" />
    } @else if (!cola.cargando() && cola.items().length === 0) {
      <bf-empty-state icon="task_alt" message="No hay bancos pendientes. ¡Todo al día!" />
    } @else {
      <ul class="lista">
        @for (banco of cola.items(); track banco.id) {
          <li class="item">
            <div class="contenido">
              <h3><a [routerLink]="['/bancos', banco.id]">{{ banco.titulo }}</a></h3>
              <p class="meta">
                {{ banco.autorNombre ?? banco.autorId }} · {{ banco.fechaSubida | date: 'short' }}
                @if (banco.ciudad) { · {{ banco.ciudad }} }
              </p>
              @if (banco.descripcion) { <p>{{ banco.descripcion }}</p> }
              @if (banco.etiquetas.length) { <p class="etiquetas">#{{ banco.etiquetas.join(' #') }}</p> }
              <a class="coords" [href]="'https://www.openstreetmap.org/?mlat=' + banco.lat + '&mlon=' + banco.lng + '#map=18/' + banco.lat + '/' + banco.lng"
                 target="_blank" rel="noopener">{{ banco.lat }}, {{ banco.lng }}</a>
            </div>
            <div class="acciones">
              <button mat-flat-button type="button" [disabled]="ocupado() === banco.id" (click)="aprobar(banco)">
                <mat-icon aria-hidden="true">check</mat-icon> Aprobar
              </button>
              <button mat-stroked-button type="button" [disabled]="ocupado() === banco.id" (click)="rechazar(banco)">
                <mat-icon aria-hidden="true">block</mat-icon> Rechazar
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
export class ColaBancos implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly notifier = inject(Notifier);

  protected readonly cola = new Cola<BancoModeracion>((page) => this.api.bancos('PENDIENTE', page));
  protected readonly ocupado = signal<string | null>(null);

  ngOnInit(): void {
    void this.cola.cargar();
  }

  protected aprobar(banco: BancoModeracion): Promise<void> {
    return this.decidir(banco, () => this.api.aprobarBanco(banco.id), `«${banco.titulo}» publicado`);
  }

  protected rechazar(banco: BancoModeracion): Promise<void> {
    return this.decidir(banco, () => this.api.rechazarBanco(banco.id), `«${banco.titulo}» rechazado`);
  }

  private async decidir(banco: BancoModeracion, accion: () => ReturnType<AdminApi['aprobarBanco']>,
      mensaje: string): Promise<void> {
    this.ocupado.set(banco.id);
    try {
      await firstValueFrom(accion());
      this.cola.quitar(banco.id);
      this.notifier.success(mensaje);
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.ocupado.set(null);
    }
  }
}
