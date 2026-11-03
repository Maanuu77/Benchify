import { NavigationError } from '@angular/router';
import { esErrorDeCarga, recargarTrasDespliegue } from './recargar-tras-despliegue';

describe('recargarTrasDespliegue', () => {
  it('recognises failed lazy chunk imports across browsers', () => {
    expect(esErrorDeCarga(new TypeError('Failed to fetch dynamically imported module: https://x/chunk-A.js'))).toBe(true);
    expect(esErrorDeCarga(new TypeError('Importing a module script failed.'))).toBe(true);
    expect(esErrorDeCarga(new TypeError('error loading dynamically imported module'))).toBe(true);
    expect(esErrorDeCarga(new Error('Cannot match any routes'))).toBe(false);
  });

  it('leaves other navigation errors alone', () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem');

    recargarTrasDespliegue(new NavigationError(1, '/mapa', new Error('boom')));

    expect(setItem).not.toHaveBeenCalled();
    setItem.mockRestore();
  });

  it('does not reload twice for the same URL', () => {
    sessionStorage.setItem('bf-recarga-tras-despliegue', '/admin');

    // Si intentara recargar, jsdom lanzaría "Not implemented: navigation"
    expect(() => recargarTrasDespliegue(new NavigationError(1, '/admin', new TypeError('Importing a module script failed.'))))
      .not.toThrow();
    sessionStorage.clear();
  });
});
