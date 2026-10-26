import { Injectable } from '@angular/core';

export interface Posicion {
  lat: number;
  lng: number;
  /** Precisión estimada en metros. */
  precision: number;
}

/** Envuelve la API de geolocalización del navegador en una Promise con mensajes en castellano. */
@Injectable({ providedIn: 'root' })
export class GeolocationService {
  isSupported(): boolean {
    return typeof navigator !== 'undefined' && 'geolocation' in navigator;
  }

  posicionActual(): Promise<Posicion> {
    if (!this.isSupported()) {
      return Promise.reject(new Error('Tu navegador no permite obtener la ubicación.'));
    }
    return new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        ({ coords }) => resolve({ lat: coords.latitude, lng: coords.longitude, precision: coords.accuracy }),
        (error) => reject(new Error(mensaje(error))),
        { enableHighAccuracy: true, timeout: 10_000, maximumAge: 30_000 },
      );
    });
  }
}

function mensaje(error: GeolocationPositionError): string {
  switch (error.code) {
    case error.PERMISSION_DENIED:
      return 'Necesitamos permiso para usar tu ubicación.';
    case error.TIMEOUT:
      return 'La ubicación está tardando demasiado. Inténtalo de nuevo.';
    default:
      return 'No hemos podido obtener tu ubicación.';
  }
}
