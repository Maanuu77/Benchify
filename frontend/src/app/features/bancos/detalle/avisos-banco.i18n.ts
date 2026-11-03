import { traducciones } from '../../../core/i18n/idioma';

export const AVISOS_BANCO = traducciones(
  {
    titulo: 'Avisar de un problema',
    queLePasa: (titulo: string) => `¿Qué le pasa a «${titulo}»? Lo revisará el equipo de moderación.`,
    tipoDeProblema: 'Tipo de problema',
    cuentanos: 'Cuéntanos qué pasa',
    detalles: 'Detalles (opcional)',
    cancelar: 'Cancelar',
    enviar: 'Enviar aviso',
    comunidadAvisa: 'La comunidad avisa:',
    avisanos: '¿Algún problema con este banco? Avísanos',
    gracias: 'Gracias por avisar. Te contaremos qué hacemos.',
    tipos: {
      ROTO: 'Está roto',
      SUCIO: 'Está sucio',
      DESAPARECIDO: 'Ya no está',
      OTRO: 'Otro problema',
    },
  },
  {
    titulo: 'Report a problem',
    queLePasa: (titulo) => `What's wrong with “${titulo}”? The moderation team will look into it.`,
    tipoDeProblema: 'Type of problem',
    cuentanos: "Tell us what's wrong",
    detalles: 'Details (optional)',
    cancelar: 'Cancel',
    enviar: 'Send report',
    comunidadAvisa: 'Reported by the community:',
    avisanos: 'Something wrong with this bench? Let us know',
    gracias: "Thanks for letting us know. We'll tell you what we do about it.",
    tipos: {
      ROTO: "It's broken",
      SUCIO: "It's dirty",
      DESAPARECIDO: "It's gone",
      OTRO: 'Another problem',
    },
  },
);
