import { inject } from '@angular/core';
import { CanActivateFn, CanMatchFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Exige sesión; si no la hay, lleva al login y vuelve después a la página pedida. */
export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.ready;
  return auth.isAuthenticated() ? true : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/**
 * Solo administradores. Es un CanMatch: el código del panel ni siquiera se descarga para el resto.
 * El backend vuelve a comprobar el rol en cada petición.
 */
export const adminGuard: CanMatchFn = async (_route, segments) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.ready;
  if (!auth.isAuthenticated()) {
    const returnUrl = '/' + segments.map((s) => s.path).join('/');
    return router.createUrlTree(['/login'], { queryParams: { returnUrl } });
  }
  return auth.isAdmin() ? true : router.createUrlTree(['/mapa']);
};

/** Las pantallas de login y registro no tienen sentido con la sesión ya iniciada. */
export const anonymousGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.ready;
  return auth.isAuthenticated() ? router.createUrlTree(['/mapa']) : true;
};
