import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_URL } from '../config/api-url';
import { params } from './http-params';
import { Notificacion, Page, PageRequest } from './models';

@Injectable({ providedIn: 'root' })
export class NotificacionesApi {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL);

  mias(page: PageRequest = {}): Observable<Page<Notificacion>> {
    return this.http.get<Page<Notificacion>>(`${this.url}/usuarios/me/notificaciones`, { params: params({ ...page }) });
  }

  noLeidas(): Observable<number> {
    return this.http.get<{ total: number }>(`${this.url}/usuarios/me/notificaciones/no-leidas`).pipe(map((r) => r.total));
  }

  marcarLeida(id: string): Observable<void> {
    return this.http.post<void>(`${this.url}/notificaciones/${id}/leida`, null);
  }

  marcarTodasLeidas(): Observable<void> {
    return this.http.post<void>(`${this.url}/usuarios/me/notificaciones/leidas`, null);
  }
}
