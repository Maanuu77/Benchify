import { computed, inject, Injectable, signal } from '@angular/core';
import {
  Auth,
  createUserWithEmailAndPassword,
  GoogleAuthProvider,
  onIdTokenChanged,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  updateProfile,
  User,
} from '@angular/fire/auth';
import { Session, toSession } from './session';

type AuthState =
  | { status: 'loading' }
  | { status: 'anonymous' }
  | { status: 'authenticated'; session: Session };

/**
 * Estado de autenticación de Firebase expuesto como signals. El rol se lee de los custom
 * claims del ID token, que es lo mismo que comprueba el backend.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly auth = inject(Auth);
  private readonly state = signal<AuthState>({ status: 'loading' });
  private resolveReady!: () => void;

  /** Se resuelve cuando Firebase ha restaurado (o descartado) la sesión guardada. */
  readonly ready = new Promise<void>((resolve) => (this.resolveReady = resolve));

  readonly session = computed(() => {
    const state = this.state();
    return state.status === 'authenticated' ? state.session : null;
  });
  readonly isLoading = computed(() => this.state().status === 'loading');
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.session()?.rol === 'ADMIN');

  constructor() {
    onIdTokenChanged(this.auth, (user) => void this.update(user));
  }

  async loginWithEmail(email: string, password: string): Promise<void> {
    await signInWithEmailAndPassword(this.auth, email, password);
  }

  async loginWithGoogle(): Promise<void> {
    await signInWithPopup(this.auth, new GoogleAuthProvider());
  }

  async register(nombre: string, email: string, password: string): Promise<void> {
    const { user } = await createUserWithEmailAndPassword(this.auth, email, password);
    await updateProfile(user, { displayName: nombre });
    // El backend toma el nombre del token: se fuerza uno nuevo que ya lo incluya
    await user.getIdToken(true);
  }

  async logout(): Promise<void> {
    await signOut(this.auth);
  }

  /** ID token vigente (Firebase lo renueva solo si va a caducar); null si no hay sesión. */
  async idToken(): Promise<string | null> {
    return this.auth.currentUser ? this.auth.currentUser.getIdToken() : null;
  }

  /** Pide un token nuevo, p. ej. tras un cambio de rol, para recibir los claims actualizados. */
  async refreshClaims(): Promise<void> {
    await this.auth.currentUser?.getIdToken(true);
  }

  private async update(user: User | null): Promise<void> {
    if (!user) {
      this.state.set({ status: 'anonymous' });
    } else {
      const { claims } = await user.getIdTokenResult();
      this.state.set({ status: 'authenticated', session: toSession(user, claims) });
    }
    this.resolveReady();
  }
}
