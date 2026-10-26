import { mensajeDeErrorAuth } from './auth-errors';
import { toSession } from './session';

describe('toSession', () => {
  const user = { uid: 'u1', email: 'a@x.com', displayName: 'Ana', photoURL: null };

  it('reads the admin role from the custom claim regardless of case', () => {
    expect(toSession(user, { role: 'ADMIN' }).rol).toBe('ADMIN');
    expect(toSession(user, { role: 'admin' }).rol).toBe('ADMIN');
  });

  it('treats a missing or unknown claim as a regular user', () => {
    expect(toSession(user, {}).rol).toBe('USER');
    expect(toSession(user, { role: 'superuser' }).rol).toBe('USER');
    expect(toSession(user, { role: true }).rol).toBe('USER');
  });

  it('keeps the profile fields of the Firebase user', () => {
    expect(toSession(user, {})).toEqual({ uid: 'u1', email: 'a@x.com', displayName: 'Ana', photoURL: null, rol: 'USER' });
  });
});

describe('mensajeDeErrorAuth', () => {
  it('translates known Firebase codes', () => {
    expect(mensajeDeErrorAuth({ code: 'auth/invalid-credential' })).toBe('Email o contraseña incorrectos.');
    expect(mensajeDeErrorAuth({ code: 'auth/email-already-in-use' })).toContain('Ya existe');
  });

  it('falls back to a generic message', () => {
    expect(mensajeDeErrorAuth(new Error('boom'))).toContain('No se ha podido');
    expect(mensajeDeErrorAuth(null)).toContain('No se ha podido');
  });
});
