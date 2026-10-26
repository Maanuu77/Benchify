const MENSAJES: Record<string, string> = {
  'auth/invalid-credential': 'Email o contraseña incorrectos.',
  'auth/invalid-email': 'El email no tiene un formato válido.',
  'auth/user-disabled': 'Esta cuenta está desactivada.',
  'auth/email-already-in-use': 'Ya existe una cuenta con ese email.',
  'auth/weak-password': 'La contraseña debe tener al menos 6 caracteres.',
  'auth/too-many-requests': 'Demasiados intentos. Espera un momento y vuelve a probar.',
  'auth/popup-closed-by-user': 'Has cerrado la ventana de Google antes de terminar.',
  'auth/popup-blocked': 'El navegador ha bloqueado la ventana de Google.',
  'auth/network-request-failed': 'No hay conexión con el servidor de autenticación.',
};

/** Traduce los códigos de error de Firebase Auth a mensajes para el usuario. */
export function mensajeDeErrorAuth(error: unknown): string {
  const code = typeof error === 'object' && error !== null && 'code' in error ? String(error.code) : '';
  return MENSAJES[code] ?? 'No se ha podido completar el inicio de sesión. Inténtalo de nuevo.';
}
