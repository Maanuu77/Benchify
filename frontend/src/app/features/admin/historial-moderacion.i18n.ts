import { traducciones } from '../../core/i18n/idioma';

export const HISTORIAL_MODERACION = traducciones(
  {
    titulo: 'Historial',
    vacio: 'Todavía no hay decisiones de moderación.',
    motivo: (motivo: string) => `«${motivo}»`,
    por: 'por',
    unModerador: 'un moderador',
    acciones: {
      BANCO_APROBADO: 'Banco publicado',
      BANCO_RECHAZADO: 'Banco rechazado',
      RESENA_ELIMINADA: 'Reseña borrada',
      RESENA_MANTENIDA: 'Reseña mantenida',
      IMAGEN_APROBADA: 'Foto aprobada',
      IMAGEN_RECHAZADA: 'Foto rechazada',
      EDICION_APROBADA: 'Edición aplicada',
      EDICION_RECHAZADA: 'Edición descartada',
    },
  },
  {
    titulo: 'History',
    vacio: 'No moderation decisions yet.',
    motivo: (motivo) => `“${motivo}”`,
    por: 'by',
    unModerador: 'a moderator',
    acciones: {
      BANCO_APROBADO: 'Bench published',
      BANCO_RECHAZADO: 'Bench rejected',
      RESENA_ELIMINADA: 'Review deleted',
      RESENA_MANTENIDA: 'Review kept',
      IMAGEN_APROBADA: 'Photo approved',
      IMAGEN_RECHAZADA: 'Photo rejected',
      EDICION_APROBADA: 'Edit applied',
      EDICION_RECHAZADA: 'Edit discarded',
    },
  },
);
