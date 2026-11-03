import { Injectable } from '@angular/core';
import { CORE } from '../i18n/core.i18n';
import { textosActuales } from '../i18n/idioma';

export interface Posicion {
  lat: number;
  lng: number;
  /** Precisión estimada en metros. */
  precision: number;
}

/** Envuelve la API de geolocalización del navegador en una Promise con mensajes en el idioma de la visita. */
@Injectable({ providedIn: 'root' })
export class GeolocationService {
  isSupported(): boolean {
    return typeof navigator !== 'undefined' && 'geolocation' in navigator;
  }

  posicionActual(): Promise<Posicion> {
    if (!this.isSupported()) {
      return Promise.reject(new Error(textosActuales(CORE).geoNoSoportada));
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
  const t = textosActuales(CORE);
  switch (error.code) {
    case error.PERMISSION_DENIED:
      return t.geoSinPermiso;
    case error.TIMEOUT:
      return t.geoLenta;
    default:
      return t.geoError;
  }
}
