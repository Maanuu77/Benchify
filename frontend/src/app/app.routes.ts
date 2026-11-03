import { Routes } from '@angular/router';
import { adminGuard, anonymousGuard, authGuard } from './core/auth/guards';
import { CORE } from './core/i18n/core.i18n';
import { textos } from './core/i18n/idioma';
import { cambiosSinGuardarGuard } from './features/bancos/formulario/cambios-sin-guardar.guard';

/** Título de la pestaña en el idioma de la visita (se resuelve al navegar, con inyección disponible). */
const titulo = (clave: keyof (typeof CORE)['es']['titulos']) => () => textos(CORE).titulos[clave];

/** Cada feature se carga bajo demanda (lazy loading) para mantener pequeño el bundle inicial. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'mapa' },
  {
    path: 'mapa',
    title: titulo('mapa'),
    loadComponent: () => import('./features/mapa/mapa-page').then((m) => m.MapaPage),
  },
  {
    path: 'bancos/nuevo',
    title: titulo('anadirBanco'),
    canActivate: [authGuard],
    runGuardsAndResolvers: 'always',
    canDeactivate: [cambiosSinGuardarGuard],
    loadComponent: () => import('./features/bancos/formulario/banco-form-page').then((m) => m.BancoFormPage),
  },
  {
    path: 'bancos/:id/editar',
    title: titulo('editarBanco'),
    canActivate: [authGuard],
    runGuardsAndResolvers: 'always',
    canDeactivate: [cambiosSinGuardarGuard],
    loadComponent: () => import('./features/bancos/formulario/banco-form-page').then((m) => m.BancoFormPage),
  },
  // Enlace para compartir: en producción lo sirve el backend con las etiquetas Open Graph; si llega aquí
  // (desarrollo, o la app ya cargada), lleva directamente a la ficha
  { path: 's/:id', redirectTo: ({ params }) => `/bancos/${params['id']}` },
  {
    path: 'bancos/:id',
    title: titulo('banco'),
    loadComponent: () => import('./features/bancos/detalle/banco-detalle-page').then((m) => m.BancoDetallePage),
  },
  {
    path: 'perfil',
    title: titulo('miPerfil'),
    canActivate: [authGuard],
    runGuardsAndResolvers: 'always',
    loadComponent: () => import('./features/perfil/perfil-page').then((m) => m.PerfilPage),
  },
  {
    path: 'usuarios/:id',
    title: titulo('perfil'),
    loadComponent: () => import('./features/perfil/perfil-page').then((m) => m.PerfilPage),
  },
  {
    path: 'admin',
    canMatch: [adminGuard],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'login',
    title: titulo('login'),
    canActivate: [anonymousGuard],
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'recuperar',
    title: titulo('recuperar'),
    canActivate: [anonymousGuard],
    loadComponent: () => import('./features/auth/recuperar-page').then((m) => m.RecuperarPage),
  },
  {
    path: 'registro',
    title: titulo('registro'),
    canActivate: [anonymousGuard],
    loadComponent: () => import('./features/auth/registro-page').then((m) => m.RegistroPage),
  },
  {
    path: '**',
    title: titulo('noEncontrada'),
    loadComponent: () => import('./features/not-found/not-found-page').then((m) => m.NotFoundPage),
  },
];
