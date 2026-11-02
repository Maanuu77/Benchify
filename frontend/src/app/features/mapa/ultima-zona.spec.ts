import { BancoResumen } from '../../core/api/models';
import { guardarUltimaZona, leerUltimaZona } from './ultima-zona';

const banco = { id: 'b1', titulo: 'Glorieta', lat: 37.38, lng: -5.99 } as BancoResumen;

describe('última zona del mapa', () => {
  afterEach(() => localStorage.clear());

  it('saves the benches with the time they were loaded', () => {
    guardarUltimaZona([banco], new Date('2026-11-02T10:00:00Z'));

    expect(leerUltimaZona()).toEqual({ fecha: '2026-11-02T10:00:00.000Z', bancos: [banco] });
  });

  it('ignores missing or corrupt data', () => {
    expect(leerUltimaZona()).toBeNull();
    localStorage.setItem('bf-ultima-zona', '{no es json');
    expect(leerUltimaZona()).toBeNull();
    localStorage.setItem('bf-ultima-zona', '{"fecha":"x"}');
    expect(leerUltimaZona()).toBeNull();
  });

  it('works without storage', () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('QuotaExceededError');
    });
    expect(() => guardarUltimaZona([banco])).not.toThrow();
    setItem.mockRestore();
  });
});
