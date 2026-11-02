import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from '../auth/auth.service';

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
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButton, MatIcon, MatMenu, MatMenuItem, MatMenuTrigger],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

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
    { path: '/mapa', label: 'Mapa', icon: 'map' },
    ...(this.auth.isAuthenticated() ? [{ path: '/bancos/nuevo', label: 'Añadir banco', icon: 'add_location_alt' }] : []),
    ...(this.auth.isAdmin() ? [{ path: '/admin', label: 'Moderación', icon: 'shield' }] : []),
  ]);

  protected readonly linksMovil = computed<NavLink[]>(() => [
    { path: '/mapa', label: 'Mapa', icon: 'map' },
    { path: '/bancos/nuevo', label: 'Añadir', icon: 'add', destacado: true },
    ...(this.auth.isAdmin() ? [{ path: '/admin', label: 'Moderar', icon: 'shield' }] : []),
    this.auth.isAuthenticated()
      ? { path: '/perfil', label: 'Perfil', icon: 'person' }
      : { path: '/login', label: 'Entrar', icon: 'login' },
  ]);

  protected readonly nombre = computed(() => {
    const session = this.auth.session();
    return session?.displayName || session?.email || 'Mi cuenta';
  });

  protected readonly iniciales = computed(() =>
    this.nombre()
      .split(/[\s@.]+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0]!.toUpperCase())
      .join(''),
  );

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/mapa');
  }
}
