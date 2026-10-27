import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatListItem, MatNavList } from '@angular/material/list';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { MatSidenav, MatSidenavContainer, MatSidenavContent } from '@angular/material/sidenav';
import { MatToolbar } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export interface NavLink {
  path: string;
  label: string;
  icon: string;
}

/** Marco de la aplicación: barra superior en escritorio y menú lateral en móvil. */
@Component({
  selector: 'bf-shell',
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbar, MatButton, MatIconButton, MatIcon,
    MatSidenavContainer, MatSidenav, MatSidenavContent, MatNavList, MatListItem,
    MatMenu, MatMenuItem, MatMenuTrigger,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly breakpoints = inject(BreakpointObserver);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly isHandset = toSignal(
    this.breakpoints.observe([Breakpoints.Handset]).pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected readonly links = computed<NavLink[]>(() => [
    { path: '/mapa', label: 'Mapa', icon: 'map' },
    ...(this.auth.isAuthenticated() ? [{ path: '/bancos/nuevo', label: 'Añadir banco', icon: 'add_location_alt' }] : []),
    ...(this.auth.isAdmin() ? [{ path: '/admin', label: 'Moderación', icon: 'admin_panel_settings' }] : []),
  ]);

  protected readonly nombre = computed(() => {
    const session = this.auth.session();
    return session?.displayName || session?.email || 'Mi cuenta';
  });

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/mapa');
  }
}
