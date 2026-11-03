import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatTabLink, MatTabNav, MatTabNavPanel } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { textos } from '../../core/i18n/idioma';
import { ADMIN_LAYOUT } from './admin-layout.i18n';

@Component({
  selector: 'bf-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatTabNav, MatTabLink, MatTabNavPanel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pagina">
      <h1>{{ t.moderacion }}</h1>
      <nav mat-tab-nav-bar [tabPanel]="panel" [attr.aria-label]="t.secciones">
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
  protected readonly t = textos(ADMIN_LAYOUT);

  protected readonly links = [
    { path: '.', label: this.t.resumen },
    { path: 'bancos', label: this.t.bancos },
    { path: 'ediciones', label: this.t.ediciones },
    { path: 'resenas', label: this.t.resenas },
    { path: 'imagenes', label: this.t.fotos },
    { path: 'incidencias', label: this.t.avisos },
    { path: 'usuarios', label: this.t.usuarios },
    { path: 'historial', label: this.t.historial },
  ];
}
