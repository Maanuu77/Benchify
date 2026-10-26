import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('renders the shell with brand, skip link and main landmark', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('.brand')?.textContent).toContain('Benchify');
    expect(element.querySelector('a.skip-link')?.getAttribute('href')).toBe('#contenido');
    expect(element.querySelector('main#contenido')).not.toBeNull();
  });
});
