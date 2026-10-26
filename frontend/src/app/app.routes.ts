import { Routes } from '@angular/router';

/** Cada feature se carga bajo demanda (lazy loading) para mantener pequeño el bundle inicial. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'mapa' },
  {
    path: 'mapa',
    title: 'Mapa · Benchify',
    loadComponent: () => import('./features/mapa/mapa-page').then((m) => m.MapaPage),
  },
  {
    path: '**',
    title: 'Página no encontrada · Benchify',
    loadComponent: () => import('./features/not-found/not-found-page').then((m) => m.NotFoundPage),
  },
];
