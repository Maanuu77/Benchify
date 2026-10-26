import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_URL } from '../config/api-url';
import { params } from './http-params';
import { CheckIn, EstadisticasUsuario, Favorito, Page, PageRequest, PerfilPublico, Usuario } from './models';

/** Perfil, favoritos y check-ins del usuario. */
@Injectable({ providedIn: 'root' })
export class UsuariosApi {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL);

  me(): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.url}/usuarios/me`);
  }

  actualizarPerfil(nombre: string, ciudad: string | null): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.url}/usuarios/me`, { nombre, ciudad });
  }

  perfil(id: string): Observable<PerfilPublico> {
    return this.http.get<PerfilPublico>(`${this.url}/usuarios/${id}`);
  }

  estadisticas(id: string): Observable<EstadisticasUsuario> {
    return this.http.get<EstadisticasUsuario>(`${this.url}/usuarios/${id}/estadisticas`);
  }

  misFavoritos(page: PageRequest = {}): Observable<Page<Favorito>> {
    return this.http.get<Page<Favorito>>(`${this.url}/usuarios/me/favoritos`, { params: params({ ...page }) });
  }

  esFavorito(bancoId: string): Observable<boolean> {
    return this.http.get<{ favorito: boolean }>(`${this.url}/bancos/${bancoId}/favorito`).pipe(map((r) => r.favorito));
  }

  marcarFavorito(bancoId: string): Observable<void> {
    return this.http.put<void>(`${this.url}/bancos/${bancoId}/favorito`, null);
  }

  desmarcarFavorito(bancoId: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/bancos/${bancoId}/favorito`);
  }

  checkIn(bancoId: string, lat: number, lng: number): Observable<CheckIn> {
    return this.http.post<CheckIn>(`${this.url}/bancos/${bancoId}/checkins`, { lat, lng });
  }

  totalCheckIns(bancoId: string): Observable<number> {
    return this.http.get<{ total: number }>(`${this.url}/bancos/${bancoId}/checkins/total`).pipe(map((r) => r.total));
  }

  misCheckIns(page: PageRequest = {}): Observable<Page<CheckIn>> {
    return this.http.get<Page<CheckIn>>(`${this.url}/usuarios/me/checkins`, { params: params({ ...page }) });
  }
}
