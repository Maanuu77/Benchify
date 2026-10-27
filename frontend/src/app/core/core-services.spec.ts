import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { Confirmation } from '../shared/ui/confirm-dialog';
import { GeolocationService } from './geo/geolocation.service';
import { Notifier } from './ui/notifier';

describe('GeolocationService', () => {
  const service = new GeolocationService();
  let getCurrentPosition: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    getCurrentPosition = vi.fn();
    vi.stubGlobal('navigator', { geolocation: { getCurrentPosition } });
  });

  afterEach(() => vi.unstubAllGlobals());

  it('resolves the device position', async () => {
    getCurrentPosition.mockImplementation((ok) => ok({ coords: { latitude: 37.38, longitude: -5.99, accuracy: 12 } }));

    await expect(service.posicionActual()).resolves.toEqual({ lat: 37.38, lng: -5.99, precision: 12 });
  });

  it('explains a denied permission in Spanish', async () => {
    getCurrentPosition.mockImplementation((_ok, ko) => ko({ code: 1, PERMISSION_DENIED: 1, TIMEOUT: 3 }));

    await expect(service.posicionActual()).rejects.toThrow('Necesitamos permiso para usar tu ubicación.');
  });

  it('explains timeouts and unsupported browsers', async () => {
    getCurrentPosition.mockImplementation((_ok, ko) => ko({ code: 3, PERMISSION_DENIED: 1, TIMEOUT: 3 }));
    await expect(service.posicionActual()).rejects.toThrow('tardando demasiado');

    vi.stubGlobal('navigator', {});
    expect(service.isSupported()).toBe(false);
    await expect(service.posicionActual()).rejects.toThrow('no permite obtener la ubicación');
  });
});

describe('Notifier', () => {
  let open: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    open = vi.fn();
    TestBed.configureTestingModule({ providers: [{ provide: MatSnackBar, useValue: { open } }] });
  });

  it('shows success messages politely', () => {
    TestBed.inject(Notifier).success('Guardado');

    expect(open).toHaveBeenCalledWith('Guardado', 'Cerrar', expect.objectContaining({ politeness: 'polite' }));
  });

  it('turns API errors into their message and announces them assertively', () => {
    TestBed.inject(Notifier).error(new HttpErrorResponse({ status: 409, error: { detail: 'Ya has reseñado este banco' } }));

    expect(open).toHaveBeenCalledWith('Ya has reseñado este banco', 'Cerrar',
      expect.objectContaining({ politeness: 'assertive', panelClass: 'snack-error' }));
  });
});

describe('Confirmation', () => {
  it('resolves true only when the dialog closes with true', async () => {
    const afterClosed = vi.fn().mockReturnValueOnce(of(true)).mockReturnValueOnce(of(undefined));
    TestBed.configureTestingModule({ providers: [{ provide: MatDialog, useValue: { open: () => ({ afterClosed }) } }] });
    const confirmation = TestBed.inject(Confirmation);

    await expect(confirmation.ask({ titulo: 't', mensaje: 'm' })).resolves.toBe(true);
    await expect(confirmation.ask({ titulo: 't', mensaje: 'm' })).resolves.toBe(false);
  });
});
