import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api-url';
import { params } from './http-params';
import { Lugar } from './models';

/** Búsqueda de direcciones (OpenStreetMap a través del backend, que guarda y reparte el cupo). */
@Injectable({ providedIn: 'root' })
export class LugaresApi {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL);

  /** @param cerca centro del mapa, para que salgan antes los resultados cercanos */
  buscar(q: string, cerca?: { lat: number; lng: number } | null): Observable<Lugar[]> {
    return this.http.get<Lugar[]>(`${this.url}/lugares`, { params: params({ q, lat: cerca?.lat, lng: cerca?.lng }) });
  }
}
