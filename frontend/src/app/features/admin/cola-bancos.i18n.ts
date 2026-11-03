import { traducciones } from '../../core/i18n/idioma';

export const COLA_BANCOS = traducciones(
  {
    titulo: 'Bancos pendientes',
    vacia: 'No hay bancos pendientes. ¡Todo al día!',
    aprobar: 'Aprobar',
    rechazar: 'Rechazar',
    publicado: (titulo: string) => `«${titulo}» publicado`,
    rechazado: (titulo: string) => `«${titulo}» rechazado`,
    rechazarBanco: 'Rechazar banco',
    rechazarMensaje: (titulo: string) => `«${titulo}» dejará de estar visible. Puedes explicar el motivo al autor.`,
    sugerencias: ['Ya existe en el mapa', 'La ubicación no es correcta', 'No es un banco público', 'Faltan datos'],
  },
  {
    titulo: 'Pending benches',
    vacia: 'No pending benches. All caught up!',
    aprobar: 'Approve',
    rechazar: 'Reject',
    publicado: (titulo) => `“${titulo}” published`,
    rechazado: (titulo) => `“${titulo}” rejected`,
    rechazarBanco: 'Reject bench',
    rechazarMensaje: (titulo) => `“${titulo}” will no longer be visible. You can explain the reason to the author.`,
    sugerencias: ['Already on the map', 'The location is wrong', 'Not a public bench', 'Missing details'],
  },
);
