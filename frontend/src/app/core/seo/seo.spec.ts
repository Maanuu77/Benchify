import { TestBed } from '@angular/core/testing';
import { Meta } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { routes } from '../../app.routes';
import { Seo } from './seo';

describe('Seo', () => {
  let seo: Seo;
  let meta: Meta;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
    meta = TestBed.inject(Meta);
    meta.updateTag({ name: 'description', content: 'Por defecto' });
    seo = TestBed.inject(Seo);
  });

  afterEach(() => document.head.querySelector('link[rel="canonical"]')?.remove());

  it('describes a page with Open Graph tags and a canonical link, then restores the defaults', () => {
    seo.describir({
      titulo: 'Glorieta · Benchify', descripcion: 'Bajo los naranjos', url: 'https://benchify.test/bancos/b1',
      imagen: 'https://benchify.test/og.png',
    });

    expect(meta.getTag('name="description"')?.content).toBe('Bajo los naranjos');
    expect(meta.getTag('property="og:title"')?.content).toBe('Glorieta · Benchify');
    expect(meta.getTag('property="og:url"')?.content).toBe('https://benchify.test/bancos/b1');
    expect(meta.getTag('property="og:image"')?.content).toBe('https://benchify.test/og.png');
    expect(document.head.querySelector('link[rel="canonical"]')?.getAttribute('href')).toBe('https://benchify.test/bancos/b1');

    seo.restablecer();

    expect(meta.getTag('name="description"')?.content).toBe('Descubre, valora y comparte bancos públicos cerca de ti.');
    expect(meta.getTag('property="og:url"')).toBeNull();
    expect(meta.getTag('property="og:image"')).toBeNull();
    expect(document.head.querySelector('link[rel="canonical"]')).toBeNull();
  });

  it('opens a shared /s/{id} link on the bench page', async () => {
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/s/b1');

    expect(router.url).toBe('/bancos/b1');
  });
});
