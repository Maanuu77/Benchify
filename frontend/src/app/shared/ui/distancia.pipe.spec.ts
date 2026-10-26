import { DistanciaPipe } from './distancia.pipe';

describe('DistanciaPipe', () => {
  const pipe = new DistanciaPipe();

  it('formats metres and kilometres in Spanish', () => {
    expect(pipe.transform(0)).toBe('0 m');
    expect(pipe.transform(412.6)).toBe('413 m');
    expect(pipe.transform(1520)).toBe('1,5 km');
    expect(pipe.transform(12_400)).toBe('12 km');
  });

  it('returns an empty string for missing values', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
    expect(pipe.transform(Number.NaN)).toBe('');
  });
});
