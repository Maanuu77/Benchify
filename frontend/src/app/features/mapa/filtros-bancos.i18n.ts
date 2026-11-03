import { traducciones } from '../../core/i18n/idioma';

export const FILTROS_BANCOS = traducciones(
  {
    filtrar: 'Filtrar bancos',
    accesible: 'Accesible',
    sombra: 'Sombra',
    respaldo: 'Respaldo',
    valoracion: 'Valoración',
    etiqueta: 'Etiqueta',
    quitar: 'Quitar',
    cualquiera: 'Cualquiera',
    oMas: (valor: string) => `${valor} o más`,
    todas: 'Todas',
  },
  {
    filtrar: 'Filter benches',
    accesible: 'Accessible',
    sombra: 'Shade',
    respaldo: 'Backrest',
    valoracion: 'Rating',
    etiqueta: 'Tag',
    quitar: 'Clear',
    cualquiera: 'Any',
    oMas: (valor) => `${valor} or more`,
    todas: 'All',
  },
);
