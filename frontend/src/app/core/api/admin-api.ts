import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api-url';
import { params } from './http-params';
import {
  BancoModeracion,
  EstadisticasModeracion,
  EstadoBanco,
  ImagenModeracion,
  Page,
  PageRequest,
  ResenaModeracion,
  Rol,
  Usuario,
} from './models';

/** Endpoints de /api/v1/admin; el backend exige rol ADMIN en todos. */
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL);

  estadisticas(): Observable<EstadisticasModeracion> {
    return this.http.get<EstadisticasModeracion>(`${this.url}/admin/estadisticas`);
  }

  bancos(estado: EstadoBanco = 'PENDIENTE', page: PageRequest = {}): Observable<Page<BancoModeracion>> {
    return this.http.get<Page<BancoModeracion>>(`${this.url}/admin/bancos`, { params: params({ estado, ...page }) });
  }

  aprobarBanco(id: string): Observable<BancoModeracion> {
    return this.http.post<BancoModeracion>(`${this.url}/admin/bancos/${id}/aprobacion`, null);
  }

  rechazarBanco(id: string): Observable<BancoModeracion> {
    return this.http.post<BancoModeracion>(`${this.url}/admin/bancos/${id}/rechazo`, null);
  }

  resenasReportadas(page: PageRequest = {}): Observable<Page<ResenaModeracion>> {
    return this.http.get<Page<ResenaModeracion>>(`${this.url}/admin/resenas/reportadas`, { params: params({ ...page }) });
  }

  eliminarResena(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/admin/resenas/${id}`);
  }

  mantenerResena(id: string): Observable<ResenaModeracion> {
    return this.http.post<ResenaModeracion>(`${this.url}/admin/resenas/${id}/revision`, null);
  }

  imagenesPendientes(page: PageRequest = {}): Observable<Page<ImagenModeracion>> {
    return this.http.get<Page<ImagenModeracion>>(`${this.url}/admin/imagenes/pendientes`, { params: params({ ...page }) });
  }

  aprobarImagen(id: string): Observable<ImagenModeracion> {
    return this.http.post<ImagenModeracion>(`${this.url}/admin/imagenes/${id}/aprobacion`, null);
  }

  rechazarImagen(id: string): Observable<void> {
    return this.http.post<void>(`${this.url}/admin/imagenes/${id}/rechazo`, null);
  }

  usuarios(page: PageRequest = {}): Observable<Page<Usuario>> {
    return this.http.get<Page<Usuario>>(`${this.url}/admin/usuarios`, { params: params({ ...page }) });
  }

  cambiarRol(usuarioId: string, rol: Rol): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.url}/admin/usuarios/${usuarioId}/rol`, { rol });
  }
}
