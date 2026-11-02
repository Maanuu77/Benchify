import { computed, inject, Injectable, signal } from '@angular/core';
import {
  Auth,
  createUserWithEmailAndPassword,
  GoogleAuthProvider,
  onIdTokenChanged,
  sendEmailVerification,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  signInWithRedirect,
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
  private version = 0;

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

  /**
   * Con ventana emergente; si el navegador la bloquea (frecuente en móviles), con redirección: la
   * página se recarga al volver de Google y la sesión llega por onIdTokenChanged.
   */
  async loginWithGoogle(): Promise<void> {
    const proveedor = new GoogleAuthProvider();
    try {
      await signInWithPopup(this.auth, proveedor);
    } catch (error) {
      if ((error as { code?: string }).code === 'auth/popup-blocked') {
        await signInWithRedirect(this.auth, proveedor);
        return;
      }
      throw error;
    }
  }

  async register(nombre: string, email: string, password: string): Promise<void> {
    const { user } = await createUserWithEmailAndPassword(this.auth, email, password);
    await updateProfile(user, { displayName: nombre });
    // El backend toma el nombre del token: se fuerza uno nuevo que ya lo incluya
    await user.getIdToken(true);
    // Sin email verificado no puede publicar: se envía el enlace nada más registrarse
    await sendEmailVerification(user).catch(() => undefined);
  }

  /** Envía el enlace para elegir una contraseña nueva. */
  async recuperarPassword(email: string): Promise<void> {
    await sendPasswordResetEmail(this.auth, email);
  }

  async enviarVerificacion(): Promise<void> {
    const user = this.auth.currentUser;
    if (user) {
      await sendEmailVerification(user);
    }
  }

  /**
   * Vuelve a leer el usuario de Firebase tras verificar el email en otra pestaña y pide un token
   * nuevo, que es el que lleva email_verified al backend. Devuelve si ya está verificado.
   */
  async comprobarVerificacion(): Promise<boolean> {
    const user = this.auth.currentUser;
    if (!user) {
      return false;
    }
    await user.reload();
    await user.getIdToken(true);
    return user.emailVerified;
  }

  async logout(): Promise<void> {
    await signOut(this.auth);
  }

  /**
   * ID token vigente; null si no hay sesión. Espera a que Firebase restaure la sesión guardada:
   * antes de eso `currentUser` es null y las primeras peticiones saldrían como anónimas.
   * Con `forceRefresh` pide uno nuevo aunque el actual no haya caducado (p. ej. tras un cambio de rol).
   */
  async idToken(forceRefresh = false): Promise<string | null> {
    await this.ready;
    const user = this.auth.currentUser;
    return user ? user.getIdToken(forceRefresh) : null;
  }

  /** Pide un token nuevo, p. ej. tras un cambio de rol, para recibir los claims actualizados. */
  async refreshClaims(): Promise<void> {
    await this.auth.currentUser?.getIdToken(true);
  }

  /** Cambia el nombre visible también en Firebase, para que la cabecera y el token lo reflejen. */
  async actualizarNombre(nombre: string): Promise<void> {
    const user = this.auth.currentUser;
    if (!user) {
      return;
    }
    await updateProfile(user, { displayName: nombre });
    // Un token nuevo dispara onIdTokenChanged y la sesión se actualiza con el nombre
    await user.getIdToken(true);
  }

  private async update(user: User | null): Promise<void> {
    // Las llamadas se solapan si el usuario cambia rápido: solo vale la última
    const version = ++this.version;
    try {
      if (!user) {
        this.state.set({ status: 'anonymous' });
        return;
      }
      let claims: Record<string, unknown> = {};
      try {
        claims = (await user.getIdTokenResult()).claims;
      } catch {
        // Sin conexión y con el token caducado: sesión con rol USER; el backend decide el rol real
      }
      if (version === this.version) {
        this.state.set({ status: 'authenticated', session: toSession(user, claims) });
      }
    } finally {
      // Aunque algo falle, los guards no pueden quedarse esperando para siempre
      this.resolveReady();
    }
  }
}
