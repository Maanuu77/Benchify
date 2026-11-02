import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api-url';
import { params } from './http-params';
import { EstadoIncidencia, Incidencia, Page, PageRequest, ResumenIncidencias, TipoIncidencia } from './models';

/** Avisos de bancos rotos, sucios o desaparecidos y su revisión en moderación. */
@Injectable({ providedIn: 'root' })
export class IncidenciasApi {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL);

  reportar(bancoId: string, tipo: TipoIncidencia, comentario?: string): Observable<Incidencia> {
    return this.http.post<Incidencia>(`${this.url}/bancos/${bancoId}/incidencias`, { tipo, comentario: comentario || null });
  }

  resumen(bancoId: string): Observable<ResumenIncidencias[]> {
    return this.http.get<ResumenIncidencias[]>(`${this.url}/bancos/${bancoId}/incidencias/resumen`);
  }

  abiertas(page: PageRequest = {}): Observable<Page<Incidencia>> {
    return this.http.get<Page<Incidencia>>(`${this.url}/admin/incidencias`, { params: params({ ...page }) });
  }

  cerrar(id: string, estado: Exclude<EstadoIncidencia, 'ABIERTA'>): Observable<Incidencia> {
    return this.http.post<Incidencia>(`${this.url}/admin/incidencias/${id}/resolucion`, { estado });
  }
}
