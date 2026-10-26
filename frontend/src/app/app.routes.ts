import { Routes } from '@angular/router';
import { anonymousGuard, authGuard } from './core/auth/guards';

/** Cada feature se carga bajo demanda (lazy loading) para mantener pequeño el bundle inicial. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'mapa' },
  {
    path: 'mapa',
    title: 'Mapa · Benchify',
    loadComponent: () => import('./features/mapa/mapa-page').then((m) => m.MapaPage),
  },
  {
    path: 'bancos/nuevo',
    title: 'Añadir banco · Benchify',
    canActivate: [authGuard],
    loadComponent: () => import('./features/bancos/formulario/banco-form-page').then((m) => m.BancoFormPage),
  },
  {
    path: 'bancos/:id/editar',
    title: 'Editar banco · Benchify',
    canActivate: [authGuard],
    loadComponent: () => import('./features/bancos/formulario/banco-form-page').then((m) => m.BancoFormPage),
  },
  {
    path: 'bancos/:id',
    title: 'Banco · Benchify',
    loadComponent: () => import('./features/bancos/detalle/banco-detalle-page').then((m) => m.BancoDetallePage),
  },
  {
    path: 'perfil',
    title: 'Mi perfil · Benchify',
    canActivate: [authGuard],
    loadComponent: () => import('./features/perfil/perfil-page').then((m) => m.PerfilPage),
  },
  {
    path: 'usuarios/:id',
    title: 'Perfil · Benchify',
    loadComponent: () => import('./features/perfil/perfil-page').then((m) => m.PerfilPage),
  },
  {
    path: 'login',
    title: 'Iniciar sesión · Benchify',
    canActivate: [anonymousGuard],
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'registro',
    title: 'Crear cuenta · Benchify',
    canActivate: [anonymousGuard],
    loadComponent: () => import('./features/auth/registro-page').then((m) => m.RegistroPage),
  },
  {
    path: '**',
    title: 'Página no encontrada · Benchify',
    loadComponent: () => import('./features/not-found/not-found-page').then((m) => m.NotFoundPage),
  },
];
