import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of, throwError } from 'rxjs';
import { API_URL } from '../config/api-url';
import { params } from './http-params';
import { Page, PageRequest, Resena, ResenaRequest } from './models';

@Injectable({ providedIn: 'root' })
export class ResenasApi {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL);

  deBanco(bancoId: string, page: PageRequest = {}): Observable<Page<Resena>> {
    return this.http.get<Page<Resena>>(`${this.url}/bancos/${bancoId}/resenas`, { params: params({ ...page }) });
  }

  /** Reseña del usuario actual para este banco, esté en la página que esté; null si no tiene. */
  mia(bancoId: string): Observable<Resena | null> {
    return this.http.get<Resena>(`${this.url}/bancos/${bancoId}/resenas/mia`).pipe(
      catchError((error: unknown) =>
        error instanceof HttpErrorResponse && error.error?.code === 'RESENA_NO_ENCONTRADA' ? of(null) : throwError(() => error),
      ),
    );
  }

  deUsuario(usuarioId: string, page: PageRequest = {}): Observable<Page<Resena>> {
    return this.http.get<Page<Resena>>(`${this.url}/usuarios/${usuarioId}/resenas`, { params: params({ ...page }) });
  }

  publicar(bancoId: string, request: ResenaRequest): Observable<Resena> {
    return this.http.post<Resena>(`${this.url}/bancos/${bancoId}/resenas`, request);
  }

  editar(id: string, request: ResenaRequest): Observable<Resena> {
    return this.http.put<Resena>(`${this.url}/resenas/${id}`, request);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/resenas/${id}`);
  }

  reportar(id: string): Observable<void> {
    return this.http.post<void>(`${this.url}/resenas/${id}/reportes`, null);
  }
}
