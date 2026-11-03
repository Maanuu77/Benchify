import { computed, effect, inject, untracked } from '@angular/core';
import { CanActivateFn, CanMatchFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Los guards solo se evalúan al navegar. Si se pierde acceso estando ya dentro (se cierra sesión en
 * otra pestaña, un administrador pierde el rol), se vuelve a navegar a la página actual para que
 * sus guards decidan. Ganar acceso (iniciar sesión) no hace nada: de eso se encarga el login.
 */
export function revalidarRutaAlPerderAcceso(): void {
  const auth = inject(AuthService);
  const router = inject(Router);
  const acceso = computed(() => {
    if (auth.isLoading()) {
      return null;
    }
    const session = auth.session();
    return session ? { uid: session.uid, admin: session.rol === 'ADMIN' } : { uid: null, admin: false };
  });
  let anterior: { uid: string | null; admin: boolean } | null = null;
  effect(() => {
    const actual = acceso();
    if (actual === null) {
      return;
    }
    const perdido =
      anterior !== null &&
      anterior.uid !== null &&
      (actual.uid !== anterior.uid || (anterior.admin && !actual.admin));
    anterior = actual;
    if (perdido) {
      untracked(() => void router.navigateByUrl(router.url, { onSameUrlNavigation: 'reload' }));
    }
  });
}

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
