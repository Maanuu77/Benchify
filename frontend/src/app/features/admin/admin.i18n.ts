import { traducciones } from '../../core/i18n/idioma';

/** Títulos de pestaña del navegador de cada sección del panel. */
export const ADMIN_TITULOS = traducciones(
  {
    resumen: 'Moderación · Benchify',
    bancos: 'Bancos pendientes · Benchify',
    resenas: 'Reseñas reportadas · Benchify',
    ediciones: 'Ediciones pendientes · Benchify',
    imagenes: 'Fotos pendientes · Benchify',
    incidencias: 'Avisos de la comunidad · Benchify',
    usuarios: 'Usuarios · Benchify',
    historial: 'Historial de moderación · Benchify',
  },
  {
    resumen: 'Moderation · Benchify',
    bancos: 'Pending benches · Benchify',
    resenas: 'Reported reviews · Benchify',
    ediciones: 'Pending edits · Benchify',
    imagenes: 'Pending photos · Benchify',
    incidencias: 'Community reports · Benchify',
    usuarios: 'Users · Benchify',
    historial: 'Moderation history · Benchify',
  },
);

/** Textos compartidos por las colas de moderación. */
export const COLA = traducciones(
  {
    cargarMas: 'Cargar más',
    verBanco: 'ver banco',
  },
  {
    cargarMas: 'Load more',
    verBanco: 'view bench',
  },
);
