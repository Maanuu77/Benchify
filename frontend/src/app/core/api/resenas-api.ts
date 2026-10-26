import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
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
