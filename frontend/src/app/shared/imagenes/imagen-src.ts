import { HttpClient } from '@angular/common/http';
import { DestroyRef, Directive, ElementRef, inject, input, output } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, map, of, switchMap } from 'rxjs';

/**
 * Pone el `src` de un `<img>`. Las fotos públicas se cargan tal cual; las que requieren
 * autenticación (fotos pendientes, que sirve la API) se descargan con HttpClient, para que el
 * interceptor añada el token, y se muestran como `blob:`.
 *
 * ```html
 * <img [bfImagenSrc]="imagen.thumbUrl" [autenticada]="imagen.requiereAutenticacion" (fallo)="…" />
 * ```
 */
@Directive({ selector: 'img[bfImagenSrc]' })
export class ImagenSrc {
  readonly bfImagenSrc = input.required<string>();
  readonly autenticada = input(false);
  /** No se ha podido descargar la foto protegida (el `<img>` emite su propio `error` para las públicas). */
  readonly fallo = output<void>();

  private readonly img = inject<ElementRef<HTMLImageElement>>(ElementRef).nativeElement;
  private readonly http = inject(HttpClient);
  private objectUrl: string | null = null;

  constructor() {
    const fuente$ = combineLatest([toObservable(this.bfImagenSrc), toObservable(this.autenticada)]).pipe(
      switchMap(([url, autenticada]) =>
        autenticada
          ? this.http.get(url, { responseType: 'blob' }).pipe(
              map((blob) => URL.createObjectURL(blob)),
              catchError(() => {
                this.fallo.emit();
                return of(null);
              }),
            )
          : of(url),
      ),
    );
    const subscription = fuente$.subscribe((src) => this.mostrar(src));
    inject(DestroyRef).onDestroy(() => {
      subscription.unsubscribe();
      this.liberar();
    });
  }

  private mostrar(src: string | null): void {
    this.liberar();
    if (src?.startsWith('blob:')) {
      this.objectUrl = src;
    }
    if (src) {
      this.img.src = src;
    }
  }

  private liberar(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}
