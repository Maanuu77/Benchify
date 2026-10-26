import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, of, throwError, toArray } from 'rxjs';
import { loadable, toApiError } from './api-error';
import { params } from './http-params';

describe('toApiError', () => {
  it('reads code, detail and field errors from a problem detail', () => {
    const error = toApiError(new HttpErrorResponse({
      status: 400,
      error: { status: 400, detail: 'Datos no válidos', code: 'VALIDATION_FAILED', errors: [{ field: 'titulo', message: 'obligatorio' }] },
    }));

    expect(error).toEqual({
      status: 400,
      code: 'VALIDATION_FAILED',
      message: 'Datos no válidos',
      fieldErrors: [{ field: 'titulo', message: 'obligatorio' }],
    });
  });

  it('explains network failures and responses without a body', () => {
    expect(toApiError(new HttpErrorResponse({ status: 0 })).code).toBe('NETWORK');
    expect(toApiError(new HttpErrorResponse({ status: 403 })).message).toBe('No tienes permiso para hacer esto.');
    expect(toApiError(new HttpErrorResponse({ status: 503 })).code).toBe('HTTP_503');
    expect(toApiError(new Error('x')).code).toBe('UNKNOWN');
  });
});

describe('loadable', () => {
  it('emits loading then the value', async () => {
    const states = await firstValueFrom(loadable(of(42)).pipe(toArray()));

    expect(states).toEqual([{ status: 'loading' }, { status: 'success', value: 42 }]);
  });

  it('emits loading then a normalized error', async () => {
    const states = await firstValueFrom(
      loadable(throwError(() => new HttpErrorResponse({ status: 404, error: { code: 'BANCO_NOT_FOUND', detail: 'No' } })))
        .pipe(toArray()));

    expect(states[1]).toMatchObject({ status: 'error', error: { status: 404, code: 'BANCO_NOT_FOUND' } });
  });
});

describe('params', () => {
  it('drops empty values so optional filters are not sent', () => {
    const result = params({ lat: 37.38, accesible: true, etiqueta: undefined, ciudad: '', page: 0, x: null });

    expect(result.toString()).toBe('lat=37.38&accesible=true&page=0');
  });
});
