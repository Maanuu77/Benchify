import { traducciones } from '../../core/i18n/idioma';

export const STAR_RATING = traducciones(
  {
    puntuacion: 'Puntuación',
    estrellas: (n: number) => `${n} ${n === 1 ? 'estrella' : 'estrellas'}`,
    deCinco: (valor: string) => `${valor} de 5 estrellas`,
  },
  {
    puntuacion: 'Rating',
    estrellas: (n) => `${n} ${n === 1 ? 'star' : 'stars'}`,
    deCinco: (valor) => `${valor} out of 5 stars`,
  },
);
