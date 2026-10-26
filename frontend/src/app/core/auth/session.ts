export type Rol = 'USER' | 'ADMIN';

/** Usuario autenticado tal y como lo ve la interfaz. */
export interface Session {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
  rol: Rol;
}

interface FirebaseUserLike {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
}

/** El rol llega en el custom claim `role` del ID token; sin claim, el usuario es USER. */
export function toSession(user: FirebaseUserLike, claims: Record<string, unknown>): Session {
  const role = typeof claims['role'] === 'string' ? claims['role'].toUpperCase() : '';
  return {
    uid: user.uid,
    email: user.email,
    displayName: user.displayName,
    photoURL: user.photoURL,
    rol: role === 'ADMIN' ? 'ADMIN' : 'USER',
  };
}
