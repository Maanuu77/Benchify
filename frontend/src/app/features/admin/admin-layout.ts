import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatTabLink, MatTabNav, MatTabNavPanel } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'bf-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatTabNav, MatTabLink, MatTabNavPanel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <h1>Moderación</h1>
      <nav mat-tab-nav-bar [tabPanel]="panel" aria-label="Secciones de moderación">
        @for (link of links; track link.path) {
          <a mat-tab-link [routerLink]="link.path" routerLinkActive #rla="routerLinkActive"
             [routerLinkActiveOptions]="{ exact: link.path === '.' }" [active]="rla.isActive">
            {{ link.label }}
          </a>
        }
      </nav>
      <mat-tab-nav-panel #panel>
        <router-outlet />
      </mat-tab-nav-panel>
    </section>
  `,
  styles: `
    .pagina {
      max-width: 1100px;
      margin: 0 auto;
      padding: 24px 16px 48px;
    }
    h1 {
      margin: 0 0 8px;
      font: var(--mat-sys-headline-medium);
    }
    mat-tab-nav-panel {
      display: block;
      padding-top: 16px;
    }
  `,
})
export class AdminLayout {
  protected readonly links = [
    { path: '.', label: 'Resumen' },
    { path: 'bancos', label: 'Bancos' },
    { path: 'resenas', label: 'Reseñas' },
    { path: 'imagenes', label: 'Fotos' },
    { path: 'usuarios', label: 'Usuarios' },
  ];
}
