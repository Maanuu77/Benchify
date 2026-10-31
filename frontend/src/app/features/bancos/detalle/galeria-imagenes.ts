import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatIconButton } from '@angular/material/button';
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
  imports: [MatIconButton, MatIcon, MatProgressBar, ImagenSrc],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="titulo-fotos">
      <div class="cabecera">
        <h2 id="titulo-fotos">Fotos <span class="cuenta">{{ imagenes().length || '' }}</span></h2>
      </div>
      @if (subiendo()) {
        <mat-progress-bar mode="indeterminate" aria-label="Subiendo foto" />
      }
      <input #selector type="file" class="oculto" [accept]="tipos" (change)="subir(selector)" aria-label="Elegir foto"
             [disabled]="!puedeSubir()" />
      @if (imagenes().length === 0 && !puedeSubir()) {
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
                <button type="button" class="miniatura" (click)="ampliar(imagen, visor)"
                        [attr.aria-label]="'Ampliar la foto ' + (i + 1)">
                  <img [bfImagenSrc]="imagen.thumbUrl" [autenticada]="imagen.requiereAutenticacion"
                       [alt]="'Foto ' + (i + 1) + ' de ' + tituloBanco()" loading="lazy"
                       (error)="marcarRota(imagen.id)" (fallo)="marcarRota(imagen.id)" />
                </button>
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
          @if (puedeSubir()) {
            <li>
              <button type="button" class="anadir" [disabled]="subiendo()" (click)="selector.click()">
                <mat-icon aria-hidden="true">add_a_photo</mat-icon>
                Añadir foto
              </button>
            </li>
          }
        </ul>
      }
    </section>

    <dialog #visor class="visor" aria-label="Foto ampliada" (close)="ampliada.set(null)">
      @if (ampliada(); as imagen) {
        <img [bfImagenSrc]="imagen.url" [autenticada]="imagen.requiereAutenticacion" [alt]="'Foto de ' + tituloBanco()" />
      }
      <button mat-icon-button type="button" class="cerrar-visor" aria-label="Cerrar" (click)="visor.close()">
        <mat-icon>close</mat-icon>
      </button>
    </dialog>
  `,
  styles: `
    h2 {
      margin: 0 0 12px;
      font: 600 18px var(--bf-font);
    }
    .cuenta {
      color: var(--bf-ink-2);
      font-weight: 500;
    }
    .oculto {
      display: none;
    }
    .vacio {
      color: var(--bf-ink-2);
    }
    .galeria {
      list-style: none;
      display: flex;
      gap: 8px;
      margin: 0 -16px;
      padding: 0 16px 4px;
      overflow-x: auto;
      scroll-snap-type: x proximity;
      scrollbar-width: thin;
    }
    li {
      position: relative;
      flex: none;
      scroll-snap-align: start;
    }
    .miniatura,
    .anadir,
    .rota {
      display: block;
      width: 160px;
      height: 120px;
      padding: 0;
      border: 0;
      border-radius: 14px;
      overflow: hidden;
      background: var(--bf-surface-2);
      cursor: zoom-in;
    }
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .anadir {
      display: grid;
      place-content: center;
      justify-items: center;
      gap: 4px;
      border: 1.5px dashed var(--bf-ink-2);
      background: transparent;
      color: var(--bf-ink-2);
      font: 600 13px var(--bf-font);
      cursor: pointer;
    }
    .rota {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 4px;
      color: var(--bf-ink-2);
      font: 500 12px var(--bf-font);
      cursor: default;
    }
    .pendiente {
      position: absolute;
      left: 8px;
      bottom: 8px;
      padding: 2px 8px;
      border-radius: var(--bf-r-pill);
      font: 600 11px var(--bf-font);
      background: var(--mat-sys-tertiary-container);
      color: var(--mat-sys-on-tertiary-container);
    }
    .borrar {
      position: absolute;
      top: 4px;
      right: 4px;
      background: var(--bf-glass);
    }
    .visor {
      max-width: min(96vw, 1200px);
      max-height: 92vh;
      padding: 0;
      border: 0;
      border-radius: var(--bf-r);
      background: #000;
      overflow: hidden;

      &::backdrop {
        background: rgb(0 0 0 / 80%);
      }
      img {
        max-height: 92vh;
        object-fit: contain;
      }
    }
    .cerrar-visor {
      position: absolute;
      top: 8px;
      right: 8px;
      background: rgb(0 0 0 / 55%);
      color: #fff;
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
  /** Foto abierta en el visor a pantalla completa. */
  protected readonly ampliada = signal<Imagen | null>(null);
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

  protected ampliar(imagen: Imagen, visor: HTMLDialogElement): void {
    this.ampliada.set(imagen);
    visor.showModal?.();
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
