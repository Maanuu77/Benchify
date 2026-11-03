import { traducciones } from '../../core/i18n/idioma';

export const COLA_IMAGENES = traducciones(
  {
    titulo: 'Fotos pendientes',
    vacia: 'No hay fotos pendientes.',
    fotoPendiente: (n: number) => `Foto pendiente ${n}`,
    seParece: (n: number) => `Se parece a ${n === 1 ? 'otra foto' : n + ' fotos'}`,
    esteBanco: 'Este banco',
    otroBanco: 'Otro banco',
    estados: { APROBADA: 'publicada', PENDIENTE: 'pendiente', RECHAZADA: 'rechazada' },
    parecida: (mismoBanco: boolean, estado: string) =>
      `Foto parecida (${mismoBanco ? 'de este banco' : 'de otro banco'}, ${estado})`,
    aprobar: 'Aprobar',
    rechazar: 'Rechazar',
    aprobada: 'Foto aprobada',
    rechazada: 'Foto rechazada y borrada',
    rechazarFoto: 'Rechazar foto',
    rechazarMensaje: 'La foto se borrará. Puedes explicar el motivo a quien la subió.',
    sugerencias: ['No se ve el banco', 'Foto borrosa u oscura', 'Aparecen personas', 'Contenido inapropiado'],
  },
  {
    titulo: 'Pending photos',
    vacia: 'No pending photos.',
    fotoPendiente: (n) => `Pending photo ${n}`,
    seParece: (n) => `Looks like ${n === 1 ? 'another photo' : n + ' photos'}`,
    esteBanco: 'This bench',
    otroBanco: 'Another bench',
    estados: { APROBADA: 'published', PENDIENTE: 'pending', RECHAZADA: 'rejected' },
    parecida: (mismoBanco, estado) => `Similar photo (${mismoBanco ? 'from this bench' : 'from another bench'}, ${estado})`,
    aprobar: 'Approve',
    rechazar: 'Reject',
    aprobada: 'Photo approved',
    rechazada: 'Photo rejected and deleted',
    rechazarFoto: 'Reject photo',
    rechazarMensaje: 'The photo will be deleted. You can explain the reason to whoever uploaded it.',
    sugerencias: ["The bench isn't visible", 'Blurry or dark photo', 'People can be seen', 'Inappropriate content'],
  },
);
