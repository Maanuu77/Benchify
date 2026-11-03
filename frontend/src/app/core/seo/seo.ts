import { DOCUMENT, inject, Injectable } from '@angular/core';
import { Meta } from '@angular/platform-browser';
import { CORE } from '../i18n/core.i18n';
import { textos } from '../i18n/idioma';

export interface Descripcion {
  titulo: string;
  descripcion: string;
  /** Dirección canónica de la página. */
  url: string;
  imagen?: string | null;
}

/**
 * Descripción y etiquetas Open Graph de la página actual. Sirven a los buscadores que ejecutan
 * JavaScript; las redes sociales leen las del enlace /s/{id}, que genera el backend.
 */
@Injectable({ providedIn: 'root' })
export class Seo {
  private readonly meta = inject(Meta);
  private readonly document = inject(DOCUMENT);
  private readonly t = textos(CORE);

  describir({ titulo, descripcion, url, imagen }: Descripcion): void {
    this.meta.updateTag({ name: 'description', content: descripcion });
    this.meta.updateTag({ property: 'og:title', content: titulo });
    this.meta.updateTag({ property: 'og:description', content: descripcion });
    this.meta.updateTag({ property: 'og:url', content: url });
    if (imagen) {
      this.meta.updateTag({ property: 'og:image', content: imagen });
    } else {
      this.meta.removeTag('property="og:image"');
    }
    this.canonica().setAttribute('href', url);
  }

  /** Vuelve a la descripción general de la aplicación (también al arrancar, para el idioma de la visita). */
  restablecer(): void {
    this.meta.updateTag({ name: 'description', content: this.t.descripcionApp });
    this.meta.updateTag({ property: 'og:title', content: this.t.tituloApp });
    this.meta.updateTag({ property: 'og:description', content: this.t.descripcionApp });
    this.meta.removeTag('property="og:url"');
    this.meta.removeTag('property="og:image"');
    this.document.head.querySelector('link[rel="canonical"]')?.remove();
  }

  private canonica(): HTMLLinkElement {
    let link = this.document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = this.document.createElement('link');
      link.rel = 'canonical';
      this.document.head.appendChild(link);
    }
    return link;
  }
}
