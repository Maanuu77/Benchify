import { traducciones } from '../i18n/idioma';

export const ERRORES = traducciones(
  {
    inesperado: 'Se ha producido un error inesperado.',
    sinConexion: 'No hay conexión con el servidor. Revisa tu red.',
    sesionCaducada: 'Tu sesión ha caducado. Vuelve a iniciar sesión.',
    sinPermiso: 'No tienes permiso para hacer esto.',
    noEncontrado: 'No hemos encontrado lo que buscas.',
    servidor: 'El servidor no está disponible ahora mismo.',
    generico: 'No se ha podido completar la operación.',
  },
  {
    inesperado: 'Something unexpected went wrong.',
    sinConexion: "Can't reach the server. Check your connection.",
    sesionCaducada: 'Your session has expired. Please sign in again.',
    sinPermiso: "You don't have permission to do this.",
    noEncontrado: "We couldn't find what you're looking for.",
    servidor: 'The server is not available right now.',
    generico: "The operation couldn't be completed.",
  },
);

/**
 * Los mensajes del backend llegan en español. En inglés se traducen por su código los que alguien
 * puede encontrarse usando la aplicación; para el resto se usa el mensaje genérico de su estado HTTP.
 */
export const MENSAJES_EN: Readonly<Record<string, string>> = {
  ADMIN_REQUIRED: 'Only a moderator can do this.',
  BANCO_DEMASIADAS_ETIQUETAS: 'A bench can have at most 5 tags.',
  BANCO_DESCRIPCION_INVALIDA: 'The description cannot be longer than 500 characters.',
  BANCO_ELIMINADO: 'This bench has been deleted.',
  BANCO_LIMITE_PENDIENTES: 'You already have 5 benches waiting for moderation; wait until they are reviewed.',
  BANCO_MAX_IMAGENES: 'This bench already has the maximum number of photos.',
  BANCO_NOT_FOUND: "We couldn't find this bench.",
  BANCO_NO_AUTOR: 'Only the author or a moderator can change this bench.',
  BANCO_NO_PUBLICADO: 'This bench is not published yet.',
  BANCO_TITULO_INVALIDO: 'The title is required and cannot be longer than 150 characters.',
  BANCO_VERSION_OBSOLETA: 'The bench has changed since you opened it; reload it and try again.',
  BUSQUEDA_INVALIDA: 'Type between 3 and 120 characters.',
  CHECKIN_DEMASIADO_LEJOS: "You're too far from the bench to check in.",
  CHECKIN_REPETIDO: "You've already checked in at this bench recently.",
  CUENTA_ELIMINADA: 'This account has been deleted.',
  DEMASIADAS_PETICIONES: 'Too many requests in a row; try again in a moment.',
  EMAIL_NO_VERIFICADO: 'Confirm your email to add benches, photos and reviews.',
  GEOCODIFICACION_NO_DISPONIBLE: 'The address search is not available right now; try again later.',
  IMAGEN_CONTENIDO_INVALIDO: "We couldn't read the image; try exporting it again as JPEG or PNG.",
  IMAGEN_DEMASIADO_GRANDE: 'The photo cannot be larger than 10 MB.',
  IMAGEN_FORMATO_NO_SOPORTADO: 'Supported formats: JPEG, PNG and WebP.',
  IMAGEN_RESOLUCION_EXCESIVA: 'The image cannot exceed 40 megapixels.',
  INCIDENCIA_DUPLICADA: 'You have already reported a problem with this bench.',
  INCIDENCIA_SIN_DETALLE: "Tell us what's wrong with the bench.",
  RESENA_BANCO_PROPIO: "You can't review a bench you added.",
  RESENA_DUPLICADA: 'You have already reviewed this bench.',
  RESENA_PROPIA: "You can't report your own review.",
  RESENA_YA_REPORTADA: 'You have already reported this review.',
  REVISION_NO_PENDIENTE: 'This edit has already been reviewed.',
  USUARIO_AUTODEGRADACION: "You can't remove your own moderator role.",
  USUARIO_NOMBRE_INVALIDO: 'The name is not valid.',
  VALIDATION_FAILED: 'Some fields are not valid.',
};
