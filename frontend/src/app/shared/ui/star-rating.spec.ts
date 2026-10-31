import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StarRating } from './star-rating';

describe('StarRating', () => {
  let fixture: ComponentFixture<StarRating>;
  let element: HTMLElement;

  beforeEach(() => {
    fixture = TestBed.createComponent(StarRating);
    element = fixture.nativeElement;
  });

  it('shows half stars and an accessible label when read-only', async () => {
    fixture.componentRef.setInput('value', 3.5);
    await fixture.whenStable();

    const icons = Array.from(element.querySelectorAll('mat-icon')).map((i) => i.textContent?.trim());
    expect(icons).toEqual(['star', 'star', 'star', 'star_half', 'star_border']);
    expect(element.querySelector('[role="img"]')?.getAttribute('aria-label')).toBe('3,5 de 5 estrellas');
  });

  it('lets the user pick a score by click and arrow keys', async () => {
    fixture.componentRef.setInput('editable', true);
    await fixture.whenStable();
    const buttons = element.querySelectorAll<HTMLButtonElement>('button[role="radio"]');

    buttons[3].click();
    await fixture.whenStable();
    expect(fixture.componentInstance.value()).toBe(4);
    expect(buttons[3].getAttribute('aria-checked')).toBe('true');

    buttons[3].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    await fixture.whenStable();
    expect(fixture.componentInstance.value()).toBe(5);

    buttons[4].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    expect(fixture.componentInstance.value()).toBe(5);
  });

  it('rounds averages to one decimal for screen readers', async () => {
    fixture.componentRef.setInput('value', 4.3333);
    await fixture.whenStable();

    expect(element.querySelector('[role="img"]')?.getAttribute('aria-label')).toBe('4,3 de 5 estrellas');
  });

  it('jumps to the ends with Home and End and exposes the invalid state', async () => {
    fixture.componentRef.setInput('editable', true);
    fixture.componentRef.setInput('invalido', true);
    fixture.componentRef.setInput('describedBy', 'error-x');
    await fixture.whenStable();
    const grupo = element.querySelector('[role="radiogroup"]')!;
    expect(grupo.getAttribute('aria-invalid')).toBe('true');
    expect(grupo.getAttribute('aria-describedby')).toBe('error-x');

    const buttons = element.querySelectorAll<HTMLButtonElement>('button[role="radio"]');
    buttons[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'End' }));
    expect(fixture.componentInstance.value()).toBe(5);
    buttons[4].dispatchEvent(new KeyboardEvent('keydown', { key: 'Home' }));
    expect(fixture.componentInstance.value()).toBe(1);
  });
});
