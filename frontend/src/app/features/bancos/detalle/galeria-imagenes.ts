import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
import { catchError, combineLatest, firstValueFrom, map, of, switchMap } from 'rxjs';
import { toApiError } from '../../../core/api/api-error';
import { BancosApi } from '../../../core/api/bancos-api';
import { Imagen } from '../../../core/api/models';
import { AuthService } from '../../../core/auth/auth.service';
import { Notifier } from '../../../core/ui/notifier';
import { comprimirImagen } from '../../../shared/imagenes/comprimir-imagen';
import { ImagenSrc } from '../../../shared/imagenes/imagen-src';
import { Confirmation } from '../../../shared/ui/confirm-dialog';

/** Resultado de pedir las fotos: la lista, o null y el mensaje si ha fallado. */
interface CargaFotos {
  lista: Imagen[] | null;
  error: string | null;
}

export const TIPOS_IMAGEN = ['image/jpeg', 'image/png', 'image/webp'];
/** Mismo límite que el servidor; se comprueba después de comprimir la foto en el navegador. */
export const MAX_BYTES_IMAGEN = 10 * 1024 * 1024;

/** Comprobaciones previas en el cliente; el backend valida además el contenido real del fichero. */
export function validarTipoImagen(file: File): string | null {
  return TIPOS_IMAGEN.includes(file.type) ? null : 'Solo se admiten fotos JPEG, PNG o WebP.';
}

export function validarTamanoImagen(file: File): string | null {
  return file.size > MAX_BYTES_IMAGEN ? 'La foto no puede superar 10 MB.' : null;
}

@Component({
  selector: 'bf-galeria-imagenes',
  imports: [MatButton, MatIconButton, MatIcon, MatProgressBar, ImagenSrc],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="titulo-fotos">
      <div class="cabecera">
        <h2 id="titulo-fotos">Fotos</h2>
        @if (puedeSubir()) {
          <input #selector type="file" class="oculto" [accept]="tipos" (change)="subir(selector)" aria-label="Elegir foto" />
          <button mat-stroked-button type="button" [disabled]="subiendo()" (click)="selector.click()">
            <mat-icon aria-hidden="true">add_a_photo</mat-icon> Añadir foto
          </button>
        }
      </div>
      @if (subiendo()) {
        <mat-progress-bar mode="indeterminate" aria-label="Subiendo foto" />
      }
      @if (imagenes().length === 0) {
        <p class="vacio">{{ error() ?? 'Este banco aún no tiene fotos.' }}</p>
      } @else {
        <ul class="galeria">
          @for (imagen of imagenes(); track imagen.id; let i = $index) {
            <li>
              @if (rotas().has(imagen.id)) {
                <div class="rota" role="img" [attr.aria-label]="'Foto ' + (i + 1) + ' no disponible'">
                  <mat-icon aria-hidden="true">broken_image</mat-icon>
                  <span>Foto no disponible</span>
                </div>
              } @else {
                <img [bfImagenSrc]="imagen.thumbUrl" [autenticada]="imagen.requiereAutenticacion"
                     [alt]="'Foto ' + (i + 1) + ' de ' + tituloBanco()" loading="lazy"
                     (error)="marcarRota(imagen.id)" (fallo)="marcarRota(imagen.id)" />
              }
              @if (imagen.estado === 'PENDIENTE') {
                <span class="pendiente">Pendiente de revisión</span>
              }
              @if (imagen.subidaPor === miUid()) {
                <button mat-icon-button type="button" class="borrar" (click)="borrar(imagen)" aria-label="Borrar foto">
                  <mat-icon>delete</mat-icon>
                </button>
              }
            </li>
          }
        </ul>
      }
    </section>
  `,
  styles: `
    .cabecera {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    h2 {
      font: var(--mat-sys-title-large);
    }
    .oculto {
      display: none;
    }
    .vacio {
      color: var(--mat-sys-on-surface-variant);
    }
    .galeria {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
      gap: 8px;
    }
    li {
      position: relative;
    }
    img {
      width: 100%;
      aspect-ratio: 4 / 3;
      object-fit: cover;
      border-radius: 8px;
      display: block;
    }
    .rota {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 4px;
      aspect-ratio: 4 / 3;
      border-radius: 8px;
      background: var(--mat-sys-surface-container-high);
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-label-medium);
    }
    .pendiente {
      position: absolute;
      left: 8px;
      bottom: 8px;
      padding: 2px 8px;
      border-radius: 4px;
      font: var(--mat-sys-label-small);
      background: var(--mat-sys-tertiary-container);
      color: var(--mat-sys-on-tertiary-container);
    }
    .borrar {
      position: absolute;
      top: 4px;
      right: 4px;
      background: var(--mat-sys-surface);
    }
  `,
})
export class GaleriaImagenes {
  private readonly api = inject(BancosApi);
  private readonly notifier = inject(Notifier);
  private readonly confirmation = inject(Confirmation);
  private readonly auth = inject(AuthService);

  readonly bancoId = input.required<string>();
  readonly tituloBanco = input('');
  readonly puedeSubir = input(false);

  protected readonly tipos = TIPOS_IMAGEN.join(',');
  protected readonly subiendo = signal(false);
  /** Imágenes cuyo fichero no se ha podido descargar (borrado o sin conexión con Storage). */
  protected readonly rotas = signal(new Set<string>());
  protected readonly miUid = computed(() => this.auth.session()?.uid ?? null);
  private readonly recarga = signal(0);

  /**
   * Depende del usuario y no de la sesión entera: la sesión cambia con cada renovación del token
   * y la galería se volvería a pedir cada hora. Solo cuenta la respuesta de la última petición.
   */
  private readonly carga = toSignal(
    combineLatest([toObservable(this.bancoId), toObservable(this.miUid), toObservable(this.recarga)]).pipe(
      switchMap(([bancoId]) =>
        this.api.imagenes(bancoId).pipe(
          map((lista): CargaFotos => ({ lista, error: null })),
          catchError((error: unknown) => of<CargaFotos>({ lista: null, error: toApiError(error).message })),
        ),
      ),
    ),
    { initialValue: { lista: [], error: null } as CargaFotos },
  );
  /** Si falla una recarga se conservan las fotos que ya había. */
  protected readonly imagenes = linkedSignal<CargaFotos, Imagen[]>({
    source: this.carga,
    computation: (carga, anterior) => carga.lista ?? anterior?.value ?? [],
  });
  protected readonly error = computed(() => this.carga().error);

  protected async subir(selector: HTMLInputElement): Promise<void> {
    const original = selector.files?.[0];
    selector.value = '';
    if (!original) return;
    const tipoInvalido = validarTipoImagen(original);
    if (tipoInvalido) {
      this.notifier.error(tipoInvalido);
      return;
    }
    this.subiendo.set(true);
    try {
      const file = await comprimirImagen(original);
      const demasiadoGrande = validarTamanoImagen(file);
      if (demasiadoGrande) {
        this.notifier.error(demasiadoGrande);
        return;
      }
      const imagen = await firstValueFrom(this.api.subirImagen(this.bancoId(), file));
      this.notifier.success(imagen.estado === 'APROBADA' ? 'Foto publicada' : 'Foto enviada: se publicará cuando la revisemos');
      this.recarga.update((n) => n + 1);
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.subiendo.set(false);
    }
  }

  protected marcarRota(id: string): void {
    this.rotas.update((actuales) => new Set(actuales).add(id));
  }

  protected async borrar(imagen: Imagen): Promise<void> {
    const ok = await this.confirmation.ask({
      titulo: 'Borrar foto', mensaje: 'La foto se eliminará definitivamente.', confirmar: 'Borrar', peligroso: true,
    });
    if (!ok) return;
    try {
      await firstValueFrom(this.api.eliminarImagen(imagen.id));
      this.imagenes.update((lista) => lista.filter((i) => i.id !== imagen.id));
    } catch (error) {
      this.notifier.error(error);
    }
  }
}
