import { TipoNotificacion } from '../api/models';
import { traducciones } from '../i18n/idioma';

export const CAMPANA = traducciones(
  {
    notificaciones: 'Notificaciones',
    sinLeer: (n: number) => `Notificaciones, ${n} sin leer`,
    marcarTodas: 'Marcar todas como leídas',
    cargando: 'Cargando…',
    vacio: 'No tienes notificaciones.',
  },
  {
    notificaciones: 'Notifications',
    sinLeer: (n) => `Notifications, ${n} unread`,
    marcarTodas: 'Mark all as read',
    cargando: 'Loading…',
    vacio: 'You have no notifications.',
  },
);

/**
 * El backend guarda las notificaciones en español, con el nombre del banco entre «». En inglés se
 * rehacen a partir del tipo y ese nombre; el motivo que escribió el moderador se deja como está.
 */
export const TITULOS_EN: Record<TipoNotificacion, (banco: string) => string> = {
  BANCO_APROBADO: (banco) => `Your bench “${banco}” is now published`,
  BANCO_RECHAZADO: (banco) => `Your bench “${banco}” has not been published`,
  FOTO_APROBADA: (banco) => `Your photo of “${banco}” is now visible`,
  FOTO_RECHAZADA: (banco) => `Your photo of “${banco}” has not been published`,
  RESENA_RETIRADA: (banco) => `We have removed your review of “${banco}”`,
  INCIDENCIA_REVISADA: (banco) => `We have reviewed your report about “${banco}”`,
  EDICION_APROBADA: (banco) => `Your changes to “${banco}” are now visible`,
  EDICION_RECHAZADA: (banco) => `We have not applied your changes to “${banco}”`,
};

/** Mensajes fijos que genera el backend (los demás son el motivo del moderador). */
export const MENSAJES_EN: Readonly<Record<string, string>> = {
  'Gracias, ya está solucionado.': "Thanks, it's been fixed.",
  'No hemos encontrado el problema.': "We couldn't find the problem.",
};
