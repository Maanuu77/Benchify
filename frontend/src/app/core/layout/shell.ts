import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatListItem, MatNavList } from '@angular/material/list';
import { MatSidenav, MatSidenavContainer, MatSidenavContent } from '@angular/material/sidenav';
import { MatToolbar } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

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
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly breakpoints = inject(BreakpointObserver);

  protected readonly isHandset = toSignal(
    this.breakpoints.observe([Breakpoints.Handset]).pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected readonly links: NavLink[] = [{ path: '/mapa', label: 'Mapa', icon: 'map' }];
}
