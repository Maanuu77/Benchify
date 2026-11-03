import { Routes } from '@angular/router';
import { textos } from '../../core/i18n/idioma';
import { ADMIN_TITULOS } from './admin.i18n';
import { AdminLayout } from './admin-layout';

/** Rutas del panel; se cargan solo si adminGuard (CanMatch) lo permite. */
export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayout,
    children: [
      { path: '', title: () => textos(ADMIN_TITULOS).resumen, loadComponent: () => import('./admin-dashboard').then((m) => m.AdminDashboard) },
      { path: 'bancos', title: () => textos(ADMIN_TITULOS).bancos, loadComponent: () => import('./cola-bancos').then((m) => m.ColaBancos) },
      { path: 'resenas', title: () => textos(ADMIN_TITULOS).resenas, loadComponent: () => import('./cola-resenas').then((m) => m.ColaResenas) },
      { path: 'ediciones', title: () => textos(ADMIN_TITULOS).ediciones, loadComponent: () => import('./cola-ediciones').then((m) => m.ColaEdiciones) },
      { path: 'imagenes', title: () => textos(ADMIN_TITULOS).imagenes, loadComponent: () => import('./cola-imagenes').then((m) => m.ColaImagenes) },
      { path: 'incidencias', title: () => textos(ADMIN_TITULOS).incidencias, loadComponent: () => import('./cola-incidencias').then((m) => m.ColaIncidencias) },
      { path: 'usuarios', title: () => textos(ADMIN_TITULOS).usuarios, loadComponent: () => import('./admin-usuarios').then((m) => m.AdminUsuarios) },
      { path: 'historial', title: () => textos(ADMIN_TITULOS).historial, loadComponent: () => import('./historial-moderacion').then((m) => m.HistorialModeracion) },
    ],
  },
];
