import { traducciones } from '../../core/i18n/idioma';

export const ADMIN_DASHBOARD = traducciones(
  {
    cargando: 'Cargando resumen…',
    error: 'No hemos podido cargar el resumen.',
    pendiente: 'Pendiente de revisar',
    bancosPendientes: (n: number) => (n === 1 ? 'banco pendiente' : 'bancos pendientes'),
    edicionesPendientes: (n: number) => (n === 1 ? 'edición pendiente' : 'ediciones pendientes'),
    resenasReportadas: (n: number) => (n === 1 ? 'reseña reportada' : 'reseñas reportadas'),
    fotosPendientes: (n: number) => (n === 1 ? 'foto pendiente' : 'fotos pendientes'),
    contenido: 'Contenido',
    bancosPublicados: 'Bancos publicados',
    bancosOcultos: 'Bancos ocultos',
    resenas: 'Reseñas',
    usuarios: 'Usuarios',
  },
  {
    cargando: 'Loading overview…',
    error: "We couldn't load the overview.",
    pendiente: 'Awaiting review',
    bancosPendientes: (n) => (n === 1 ? 'pending bench' : 'pending benches'),
    edicionesPendientes: (n) => (n === 1 ? 'pending edit' : 'pending edits'),
    resenasReportadas: (n) => (n === 1 ? 'reported review' : 'reported reviews'),
    fotosPendientes: (n) => (n === 1 ? 'pending photo' : 'pending photos'),
    contenido: 'Content',
    bancosPublicados: 'Published benches',
    bancosOcultos: 'Hidden benches',
    resenas: 'Reviews',
    usuarios: 'Users',
  },
);
