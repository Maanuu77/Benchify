import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_URL } from '../config/api-url';
import { params } from './http-params';
import {
  Banco,
  BancoCercano,
  BancoRequest,
  BancoResumen,
  BoundingBox,
  Etiqueta,
  FiltroBancos,
  Imagen,
  Page,
  PageRequest,
} from './models';

@Injectable({ providedIn: 'root' })
export class BancosApi {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL);

  /** El catálogo cambia solo con migraciones: se pide una vez por sesión. */
  private readonly etiquetas$ = this.http.get<Etiqueta[]>(`${this.url}/etiquetas`).pipe(shareReplay(1));

  enArea(area: BoundingBox, filtro: FiltroBancos = {}): Observable<BancoResumen[]> {
    return this.http.get<BancoResumen[]>(`${this.url}/bancos`, { params: params({ ...area, ...filtro }) });
  }

  cercanos(lat: number, lng: number, radio: number, filtro: FiltroBancos = {}, page: PageRequest = {}):
      Observable<Page<BancoCercano>> {
    return this.http.get<Page<BancoCercano>>(`${this.url}/bancos/cercanos`, {
      params: params({ lat, lng, radio, ...filtro, ...page }),
    });
  }

  obtener(id: string): Observable<Banco> {
    return this.http.get<Banco>(`${this.url}/bancos/${id}`);
  }

  proponer(request: BancoRequest): Observable<Banco> {
    return this.http.post<Banco>(`${this.url}/bancos`, request);
  }

  editar(id: string, request: BancoRequest): Observable<Banco> {
    return this.http.put<Banco>(`${this.url}/bancos/${id}`, request);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/bancos/${id}`);
  }

  deUsuario(usuarioId: string, page: PageRequest = {}): Observable<Page<BancoResumen>> {
    return this.http.get<Page<BancoResumen>>(`${this.url}/usuarios/${usuarioId}/bancos`, { params: params({ ...page }) });
  }

  etiquetas(): Observable<Etiqueta[]> {
    return this.etiquetas$;
  }

  imagenes(bancoId: string): Observable<Imagen[]> {
    return this.http.get<Imagen[]>(`${this.url}/bancos/${bancoId}/imagenes`);
  }

  subirImagen(bancoId: string, archivo: File): Observable<Imagen> {
    const body = new FormData();
    body.append('archivo', archivo, archivo.name);
    return this.http.post<Imagen>(`${this.url}/bancos/${bancoId}/imagenes`, body);
  }

  eliminarImagen(imagenId: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/imagenes/${imagenId}`);
  }
}
