import { TestBed } from '@angular/core/testing';
import { Auth, User } from '@angular/fire/auth';
import { AuthService } from './auth.service';

/** Auth de Firebase mínimo: guarda el listener de onIdTokenChanged para dispararlo a mano. */
class FakeFirebaseAuth {
  currentUser: Partial<User> | null = null;
  private listener: ((user: Partial<User> | null) => void) | null = null;

  onIdTokenChanged(listener: (user: Partial<User> | null) => void): () => void {
    this.listener = listener;
    return () => (this.listener = null);
  }

  emit(user: Partial<User> | null): void {
    this.currentUser = user;
    this.listener?.(user);
  }
}

function user(uid: string, claims: Promise<Record<string, unknown>>): Partial<User> {
  return {
    uid,
    email: `${uid}@benchify.dev`,
    displayName: uid,
    photoURL: null,
    getIdToken: vi.fn(async () => `token-${uid}`),
    getIdTokenResult: vi.fn(async () => ({ claims: await claims })) as unknown as User['getIdTokenResult'],
  };
}

describe('AuthService', () => {
  let firebase: FakeFirebaseAuth;
  let service: AuthService;

  beforeEach(() => {
    firebase = new FakeFirebaseAuth();
    TestBed.configureTestingModule({ providers: [{ provide: Auth, useValue: firebase }] });
    service = TestBed.inject(AuthService);
  });

  it('waits for the restored session before handing out a token', async () => {
    const token = service.idToken();
    firebase.emit(user('ana', Promise.resolve({ role: 'admin' })));

    await expect(token).resolves.toBe('token-ana');
    expect(service.isAdmin()).toBe(true);
  });

  it('becomes ready even if the token claims cannot be read', async () => {
    firebase.emit(user('ana', Promise.reject(new Error('offline'))));

    await service.ready;
    expect(service.session()).toMatchObject({ uid: 'ana', rol: 'USER' });
  });

  it('keeps only the latest session when updates overlap', async () => {
    let resolveSlow!: (claims: Record<string, unknown>) => void;
    firebase.emit(user('ana', new Promise((resolve) => (resolveSlow = resolve))));
    firebase.emit(null);
    await service.ready;

    resolveSlow({ role: 'admin' });
    await new Promise((resolve) => setTimeout(resolve));
    expect(service.isAuthenticated()).toBe(false);
  });
});
