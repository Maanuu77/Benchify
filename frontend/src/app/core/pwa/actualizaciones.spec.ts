import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SwUpdate, VersionEvent } from '@angular/service-worker';
import { Subject } from 'rxjs';
import { Actualizaciones, INTERVALO_COMPROBACION_MS } from './actualizaciones';

describe('Actualizaciones', () => {
  let versionUpdates: Subject<VersionEvent>;
  let unrecoverable: Subject<unknown>;
  let sw: { isEnabled: boolean; versionUpdates: Subject<VersionEvent>; unrecoverable: Subject<unknown>; checkForUpdate: ReturnType<typeof vi.fn> };
  let accion: Subject<void>;
  let snackBar: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    vi.useFakeTimers();
    versionUpdates = new Subject();
    unrecoverable = new Subject();
    sw = { isEnabled: true, versionUpdates, unrecoverable, checkForUpdate: vi.fn(async () => false) };
    accion = new Subject();
    snackBar = { open: vi.fn(() => ({ onAction: () => accion })) };
    TestBed.configureTestingModule({
      providers: [{ provide: SwUpdate, useValue: sw }, { provide: MatSnackBar, useValue: snackBar }],
    });
  });

  afterEach(() => vi.useRealTimers());

  it('offers to reload once a new version is ready, and only then', () => {
    TestBed.inject(Actualizaciones).iniciar();

    versionUpdates.next({ type: 'VERSION_DETECTED', version: { hash: 'b' } });
    expect(snackBar.open).not.toHaveBeenCalled();

    versionUpdates.next({ type: 'VERSION_READY', currentVersion: { hash: 'a' }, latestVersion: { hash: 'b' } });
    expect(snackBar.open).toHaveBeenCalledWith('Hay una versión nueva de Benchify', 'Actualizar', expect.anything());
  });

  it('asks to reload when the cached version is broken', () => {
    TestBed.inject(Actualizaciones).iniciar();

    unrecoverable.next({ type: 'UNRECOVERABLE_STATE', reason: 'missing files' });

    expect(snackBar.open).toHaveBeenCalledWith(expect.stringContaining('recargarse'), 'Recargar', expect.anything());
  });

  it('checks for updates periodically while the app stays open', () => {
    TestBed.inject(Actualizaciones).iniciar();

    vi.advanceTimersByTime(INTERVALO_COMPROBACION_MS * 2);

    expect(sw.checkForUpdate).toHaveBeenCalledTimes(2);
  });

  it('does nothing without a service worker (development, unsupported browsers)', () => {
    sw.isEnabled = false;
    TestBed.inject(Actualizaciones).iniciar();

    versionUpdates.next({ type: 'VERSION_READY', currentVersion: { hash: 'a' }, latestVersion: { hash: 'b' } });
    vi.advanceTimersByTime(INTERVALO_COMPROBACION_MS);

    expect(snackBar.open).not.toHaveBeenCalled();
    expect(sw.checkForUpdate).not.toHaveBeenCalled();
  });
});
