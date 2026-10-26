import { computed, signal } from '@angular/core';
import { Session } from '../session';

/** Sustituto de AuthService para tests: la sesión se fija a mano. */
export class FakeAuthService {
  private readonly current = signal<Session | null>(null);

  readonly ready = Promise.resolve();
  readonly session = this.current.asReadonly();
  readonly isLoading = signal(false).asReadonly();
  readonly isAuthenticated = computed(() => this.current() !== null);
  readonly isAdmin = computed(() => this.current()?.rol === 'ADMIN');

  token: string | null = null;
  readonly loginWithEmail = vi.fn(async () => undefined);
  readonly loginWithGoogle = vi.fn(async () => undefined);
  readonly register = vi.fn(async () => undefined);
  readonly logout = vi.fn(async () => this.signOut());
  readonly refreshClaims = vi.fn(async () => undefined);

  signIn(partial: Partial<Session> = {}): void {
    this.current.set({ uid: 'ana', email: 'ana@benchify.dev', displayName: 'Ana', photoURL: null, rol: 'USER', ...partial });
    this.token = 'token-' + (partial.uid ?? 'ana');
  }

  signOut(): void {
    this.current.set(null);
    this.token = null;
  }

  async idToken(): Promise<string | null> {
    return this.token;
  }
}
