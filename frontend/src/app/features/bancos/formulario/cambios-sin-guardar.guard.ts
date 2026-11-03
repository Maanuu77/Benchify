import { CanDeactivateFn } from '@angular/router';

/** Pantalla con un formulario que puede tener cambios sin guardar. */
export interface ConCambiosSinGuardar {
  /** true si se puede salir; si hay cambios, pregunta antes. */
  puedeSalir(): boolean | Promise<boolean>;
}

/** Evita perder lo escrito al navegar fuera del formulario por error. */
export const cambiosSinGuardarGuard: CanDeactivateFn<ConCambiosSinGuardar> = (componente) => componente.puedeSalir();
