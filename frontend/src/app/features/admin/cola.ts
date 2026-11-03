import { signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { Page, PageRequest } from '../../core/api/models';

/**
 * Cola de moderación paginada: carga páginas bajo demanda y quita los elementos ya resueltos
 * sin volver a pedir la lista.
 *
 * Lo que se resuelve sale también de la cola del servidor, que se desplaza: la página siguiente no
 * es "la anterior + 1" sino la que empieza donde acaban los elementos que siguen visibles. Como la
 * API pagina por número de página, puede repetir alguno de los ya cargados; esos se descartan.
 */
export class Cola<T extends { id: string }> {
  readonly items = signal<T[]>([]);
  readonly total = signal(0);
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);

  constructor(private readonly fuente: (page: PageRequest) => Observable<Page<T>>, private readonly tamano = 20) {}

  async cargar(): Promise<void> {
    this.items.set([]);
    await this.cargarPagina(0);
  }

  async cargarMas(): Promise<void> {
    await this.cargarPagina(Math.floor(this.items().length / this.tamano));
  }

  hayMas(): boolean {
    return this.items().length < this.total();
  }

  /** Quita un elemento resuelto; si la lista se queda vacía y quedan más, carga los siguientes. */
  quitar(id: string): void {
    this.items.update((items) => items.filter((item) => item.id !== id));
    this.total.update((total) => Math.max(0, total - 1));
    if (this.items().length === 0 && this.hayMas() && !this.cargando()) {
      void this.cargar();
    }
  }

  reemplazar(item: T): void {
    this.items.update((items) => items.map((actual) => (actual.id === item.id ? item : actual)));
  }

  private async cargarPagina(pagina: number): Promise<void> {
    this.cargando.set(true);
    this.error.set(null);
    try {
      const page = await firstValueFrom(this.fuente({ page: pagina, size: this.tamano }));
      this.items.update((items) => {
        const cargados = new Set(items.map((item) => item.id));
        return [...items, ...page.content.filter((item) => !cargados.has(item.id))];
      });
      this.total.set(page.totalElements);
    } catch (error) {
      this.error.set(toApiError(error).message);
    } finally {
      this.cargando.set(false);
    }
  }
}
