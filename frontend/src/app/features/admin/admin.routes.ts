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
      { path: 'imagenes', title: 'Fotos pendientes · Benchify', loadComponent: () => import('./cola-imagenes').then((m) => m.ColaImagenes) },
      { path: 'usuarios', title: 'Usuarios · Benchify', loadComponent: () => import('./admin-usuarios').then((m) => m.AdminUsuarios) },
    ],
  },
];
