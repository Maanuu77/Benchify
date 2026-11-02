import { Routes } from '@angular/router';
import { AdminLayout } from './admin-layout';

/** Rutas del panel; se cargan solo si adminGuard (CanMatch) lo permite. */
export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayout,
    children: [
      { path: '', title: 'Moderación · Benchify', loadComponent: () => import('./admin-dashboard').then((m) => m.AdminDashboard) },
      { path: 'bancos', title: 'Bancos pendientes · Benchify', loadComponent: () => import('./cola-bancos').then((m) => m.ColaBancos) },
      { path: 'resenas', title: 'Reseñas reportadas · Benchify', loadComponent: () => import('./cola-resenas').then((m) => m.ColaResenas) },
      { path: 'ediciones', title: 'Ediciones pendientes · Benchify', loadComponent: () => import('./cola-ediciones').then((m) => m.ColaEdiciones) },
      { path: 'imagenes', title: 'Fotos pendientes · Benchify', loadComponent: () => import('./cola-imagenes').then((m) => m.ColaImagenes) },
      { path: 'incidencias', title: 'Avisos de la comunidad · Benchify', loadComponent: () => import('./cola-incidencias').then((m) => m.ColaIncidencias) },
      { path: 'usuarios', title: 'Usuarios · Benchify', loadComponent: () => import('./admin-usuarios').then((m) => m.AdminUsuarios) },
      { path: 'historial', title: 'Historial de moderación · Benchify', loadComponent: () => import('./historial-moderacion').then((m) => m.HistorialModeracion) },
    ],
  },
];
