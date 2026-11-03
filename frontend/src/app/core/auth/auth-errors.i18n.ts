import { traducciones } from '../i18n/idioma';

export const ERRORES_AUTH = traducciones(
  {
    codigos: {
      'auth/invalid-credential': 'Email o contraseña incorrectos.',
      'auth/invalid-email': 'El email no tiene un formato válido.',
      'auth/user-disabled': 'Esta cuenta está desactivada.',
      'auth/email-already-in-use': 'Ya existe una cuenta con ese email.',
      'auth/weak-password': 'La contraseña debe tener al menos 6 caracteres.',
      'auth/too-many-requests': 'Demasiados intentos. Espera un momento y vuelve a probar.',
      'auth/popup-closed-by-user': 'Has cerrado la ventana de Google antes de terminar.',
      'auth/popup-blocked': 'El navegador ha bloqueado la ventana de Google.',
      'auth/network-request-failed': 'No hay conexión con el servidor de autenticación.',
    },
    generico: 'No se ha podido completar el inicio de sesión. Inténtalo de nuevo.',
  },
  {
    codigos: {
      'auth/invalid-credential': 'Incorrect email or password.',
      'auth/invalid-email': "The email address isn't valid.",
      'auth/user-disabled': 'This account has been disabled.',
      'auth/email-already-in-use': 'There is already an account with that email.',
      'auth/weak-password': 'The password must be at least 6 characters long.',
      'auth/too-many-requests': 'Too many attempts. Wait a moment and try again.',
      'auth/popup-closed-by-user': 'You closed the Google window before finishing.',
      'auth/popup-blocked': 'The browser blocked the Google window.',
      'auth/network-request-failed': "Can't reach the authentication server.",
    },
    generico: "We couldn't complete the sign-in. Please try again.",
  },
);
