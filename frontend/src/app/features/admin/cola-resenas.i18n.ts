import { traducciones } from '../../core/i18n/idioma';

export const COLA_RESENAS = traducciones(
  {
    titulo: 'Reseñas reportadas',
    vacia: 'No hay reseñas reportadas.',
    reportes: (n: number) => `${n} ${n === 1 ? 'reporte' : 'reportes'}`,
    sinComentario: '(sin comentario)',
    mantener: 'Mantener',
    borrar: 'Borrar',
    mantenida: 'Reseña mantenida',
    borrada: 'Reseña borrada',
    borrarResena: 'Borrar reseña',
    borrarMensaje: 'Se borrará y se recalculará la valoración del banco. Su autor recibirá un aviso.',
    sugerencias: ['Lenguaje ofensivo', 'Spam o publicidad', 'No habla del banco'],
  },
  {
    titulo: 'Reported reviews',
    vacia: 'No reported reviews.',
    reportes: (n) => `${n} ${n === 1 ? 'report' : 'reports'}`,
    sinComentario: '(no comment)',
    mantener: 'Keep',
    borrar: 'Delete',
    mantenida: 'Review kept',
    borrada: 'Review deleted',
    borrarResena: 'Delete review',
    borrarMensaje: "It will be deleted and the bench's rating recalculated. Its author will be notified.",
    sugerencias: ['Offensive language', 'Spam or advertising', "Isn't about the bench"],
  },
);
