import { Component, input, output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { BancosApi } from '../../core/api/bancos-api';
import { BancoResumen, BoundingBox } from '../../core/api/models';
import { GeolocationService } from '../../core/geo/geolocation.service';
import { Notifier } from '../../core/ui/notifier';
import { BenchMap, Punto } from '../../shared/map/bench-map';
import { MapaPage, RADIO_CERCANIA } from './mapa-page';

@Component({ selector: 'bf-bench-map', template: '' })
class BenchMapStub {
  readonly bancos = input<BancoResumen[]>([]);
  readonly centro = input<Punto>();
  readonly zoom = input(14);
  readonly usuario = input<Punto | null>(null);
  readonly areaChange = output<BoundingBox>();
  readonly bancoSelect = output<BancoResumen>();
  volarA = vi.fn();
}

const banco = (id: string, puntuacionMedia: number, extra: Partial<BancoResumen> = {}): BancoResumen => ({
  id, titulo: 'Banco ' + id, lat: 37.38, lng: -5.99, ciudad: 'Sevilla', estado: 'PUBLICADO',
  puntuacionMedia, numResenas: 1, accesible: false, conSombra: false, conRespaldo: false, etiquetas: [], ...extra,
});

const AREA: BoundingBox = { minLat: 37.3, minLng: -6.1, maxLat: 37.4, maxLng: -5.9 };

describe('MapaPage', () => {
  let fixture: ComponentFixture<MapaPage>;
  let element: HTMLElement;
  let api: { enArea: ReturnType<typeof vi.fn>; cercanos: ReturnType<typeof vi.fn>; etiquetas: ReturnType<typeof vi.fn> };
  let geo: { posicionActual: ReturnType<typeof vi.fn> };
  let notifier: { error: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    vi.useFakeTimers();
    api = {
      enArea: vi.fn(() => of([banco('a', 3), banco('b', 4.5)])),
      cercanos: vi.fn(),
      etiquetas: vi.fn(() => of([])),
    };
    geo = { posicionActual: vi.fn() };
    notifier = { error: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: BancosApi, useValue: api },
        { provide: GeolocationService, useValue: geo },
        { provide: Notifier, useValue: notifier },
      ],
    });
    TestBed.overrideComponent(MapaPage, { remove: { imports: [BenchMap] }, add: { imports: [BenchMapStub] } });
    fixture = TestBed.createComponent(MapaPage);
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  async function moverMapa(area = AREA): Promise<void> {
    fixture.debugElement.query((d) => d.componentInstance instanceof BenchMapStub)
      .componentInstance.areaChange.emit(area);
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();
  }

  it('loads the visible area after the map settles and lists the best rated first', async () => {
    await moverMapa();

    expect(api.enArea).toHaveBeenCalledWith(AREA, {});
    expect(element.textContent).toContain('2 bancos en esta zona');
    const titulos = Array.from(element.querySelectorAll('[matListItemTitle]')).map((e) => e.textContent?.trim());
    expect(titulos).toEqual(['Banco b', 'Banco a']);
  });

  it('shows an empty state when nothing matches', async () => {
    api.enArea.mockReturnValue(of([]));
    await moverMapa();

    expect(element.textContent).toContain('No hay bancos aquí con estos filtros');
  });

  it('shows an error state when the API fails', async () => {
    api.enArea.mockReturnValue(throwError(() => new Error('down')));
    await moverMapa();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('No hemos podido cargar');
  });

  it('searches nearby benches from the user position', async () => {
    geo.posicionActual.mockResolvedValue({ lat: 37.38, lng: -5.99, precision: 10 });
    api.cercanos.mockReturnValue(of({
      content: [{ banco: banco('c', 4), distanciaMetros: 230 }], page: 0, size: 20, totalElements: 1, totalPages: 1,
    }));
    await moverMapa();

    (element.querySelector('.panel-header button') as HTMLButtonElement).click();
    await vi.runAllTimersAsync();
    fixture.detectChanges();

    expect(api.cercanos).toHaveBeenCalledWith(37.38, -5.99, RADIO_CERCANIA, {}, { size: 20 });
    expect(element.textContent).toContain('Cerca de ti');
    expect(element.textContent).toContain('230 m');
  });

  it('reports a denied geolocation without breaking the page', async () => {
    geo.posicionActual.mockRejectedValue(new Error('Necesitamos permiso para usar tu ubicación.'));

    (element.querySelector('.panel-header button') as HTMLButtonElement).click();
    await vi.runAllTimersAsync();

    expect(notifier.error).toHaveBeenCalledWith('Necesitamos permiso para usar tu ubicación.');
    expect(api.cercanos).not.toHaveBeenCalled();
  });
});
