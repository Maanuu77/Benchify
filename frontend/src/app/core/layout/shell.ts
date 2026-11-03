import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { textos } from '../i18n/idioma';
import { SelectorIdioma } from '../i18n/selector-idioma';
import { NotificacionesService } from '../notificaciones/notificaciones.service';
import { Notifier } from '../ui/notifier';
import { CampanaNotificaciones } from './campana-notificaciones';
import { SHELL } from './shell.i18n';

export interface NavLink {
  path: string;
  label: string;
  icon: string;
  /** Acción principal, resaltada en la barra inferior. */
  destacado?: boolean;
}

/** Ancho a partir del cual se usa la cabecera con enlaces en lugar de la barra inferior. */
export const BREAKPOINT_MOVIL = '(max-width: 767.98px)';

/**
 * Marco de la aplicación. En escritorio, cabecera con la navegación; en móvil, barra inferior
 * y, en el mapa, ninguna cabecera: el mapa ocupa la pantalla y sus controles flotan encima.
 */
@Component({
  selector: 'bf-shell',
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, MatButton, MatIcon, MatMenu, MatMenuItem, MatMenuTrigger,
    CampanaNotificaciones, SelectorIdioma,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly router = inject(Router);
  private readonly notifier = inject(Notifier);
  protected readonly notificaciones = inject(NotificacionesService);
  protected readonly auth = inject(AuthService);
  protected readonly t = textos(SHELL);

  protected readonly isHandset = toSignal(
    inject(BreakpointObserver).observe(BREAKPOINT_MOVIL).pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );
  protected readonly esMapa = computed(() => /^\/mapa(?:[?#/]|$)/.test(this.url()) || this.url() === '/');

  protected readonly links = computed<NavLink[]>(() => [
    { path: '/mapa', label: this.t.mapa, icon: 'map' },
    ...(this.auth.isAuthenticated() ? [{ path: '/bancos/nuevo', label: this.t.anadirBanco, icon: 'add_location_alt' }] : []),
    ...(this.auth.isAdmin() ? [{ path: '/admin', label: this.t.moderacion, icon: 'shield' }] : []),
  ]);

  protected readonly linksMovil = computed<NavLink[]>(() => [
    { path: '/mapa', label: this.t.mapa, icon: 'map' },
    { path: '/bancos/nuevo', label: this.t.anadir, icon: 'add', destacado: true },
    ...(this.auth.isAdmin() ? [{ path: '/admin', label: this.t.moderar, icon: 'shield' }] : []),
    this.auth.isAuthenticated()
      ? { path: '/perfil', label: this.t.perfil, icon: 'person' }
      : { path: '/login', label: this.t.entrar, icon: 'login' },
  ]);

  protected readonly nombre = computed(() => {
    const session = this.auth.session();
    return session?.displayName || session?.email || this.t.miCuenta;
  });

  protected readonly iniciales = computed(() =>
    this.nombre()
      .split(/[\s@.]+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0]!.toUpperCase())
      .join(''),
  );

  protected readonly verificando = signal(false);

  protected async reenviarVerificacion(): Promise<void> {
    await this.conVerificacion(async () => {
      await this.auth.enviarVerificacion();
      this.notifier.success(this.t.enlaceReenviado);
    });
  }

  /** Tras pulsar el enlace en otra pestaña, relee el usuario para quitar el aviso sin cerrar sesión. */
  protected async comprobarVerificacion(): Promise<void> {
    await this.conVerificacion(async () => {
      if (!(await this.auth.comprobarVerificacion())) {
        this.notifier.error(this.t.aunSinConfirmar);
      }
    });
  }

  private async conVerificacion(accion: () => Promise<void>): Promise<void> {
    this.verificando.set(true);
    try {
      await accion();
    } catch (error) {
      this.notifier.error(error);
    } finally {
      this.verificando.set(false);
    }
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/mapa');
  }
}
