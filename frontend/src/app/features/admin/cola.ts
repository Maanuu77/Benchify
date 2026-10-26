import { signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { toApiError } from '../../core/api/api-error';
import { Page, PageRequest } from '../../core/api/models';

/**
 * Cola de moderación paginada: carga páginas bajo demanda y quita los elementos ya resueltos
 * sin volver a pedir la lista.
 */
export class Cola<T extends { id: string }> {
  readonly items = signal<T[]>([]);
  readonly total = signal(0);
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  private pagina = 0;

  constructor(private readonly fuente: (page: PageRequest) => Observable<Page<T>>, private readonly tamano = 20) {}

  async cargar(): Promise<void> {
    this.pagina = 0;
    this.items.set([]);
    await this.cargarPagina(0);
  }

  async cargarMas(): Promise<void> {
    await this.cargarPagina(this.pagina + 1);
  }

  hayMas(): boolean {
    return this.items().length < this.total();
  }

  quitar(id: string): void {
    this.items.update((items) => items.filter((item) => item.id !== id));
    this.total.update((total) => Math.max(0, total - 1));
  }

  reemplazar(item: T): void {
    this.items.update((items) => items.map((actual) => (actual.id === item.id ? item : actual)));
  }

  private async cargarPagina(pagina: number): Promise<void> {
    this.cargando.set(true);
    this.error.set(null);
    try {
      const page = await firstValueFrom(this.fuente({ page: pagina, size: this.tamano }));
      this.items.update((items) => (pagina === 0 ? page.content : [...items, ...page.content]));
      this.total.set(page.totalElements);
      this.pagina = pagina;
    } catch (error) {
      this.error.set(toApiError(error).message);
    } finally {
      this.cargando.set(false);
    }
  }
}
