import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Notificacion } from '../api/models';
import { NotificacionesService } from '../notificaciones/notificaciones.service';
import { CampanaNotificaciones } from './campana-notificaciones';

const aviso = (id: string, extra: Partial<Notificacion> = {}): Notificacion => ({
  id, tipo: 'BANCO_APROBADO', titulo: 'Tu banco «Glorieta» ya está publicado', mensaje: null, enlace: '/bancos/b1',
  leida: false, fecha: '2026-11-01T10:00:00Z', ...extra,
});

describe('CampanaNotificaciones', () => {
  let fixture: ComponentFixture<CampanaNotificaciones>;
  let servicio: {
    noLeidas: ReturnType<typeof signal<number>>;
    recientes: ReturnType<typeof vi.fn>;
    marcarLeida: ReturnType<typeof vi.fn>;
    marcarTodasLeidas: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    servicio = {
      noLeidas: signal(2),
      recientes: vi.fn(async () => [aviso('n1'), aviso('n2', { leida: true, tipo: 'FOTO_RECHAZADA', mensaje: 'Borrosa' })]),
      marcarLeida: vi.fn(async () => undefined),
      marcarTodasLeidas: vi.fn(async () => undefined),
    };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: NotificacionesService, useValue: servicio }],
    });
    fixture = TestBed.createComponent(CampanaNotificaciones);
    await fixture.whenStable();
  });

  function abrirMenu(): Promise<void> {
    (fixture.nativeElement.querySelector('.campana') as HTMLButtonElement).click();
    return fixture.whenStable();
  }

  it('shows the unread count in the button and its label', () => {
    const boton = fixture.nativeElement.querySelector('.campana') as HTMLButtonElement;

    expect(boton.querySelector('.contador')?.textContent).toBe('2');
    expect(boton.getAttribute('aria-label')).toBe('Notificaciones, 2 sin leer');
  });

  it('lists the latest notifications and opens one marking it as read', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await abrirMenu();

    const items = Array.from(document.querySelectorAll<HTMLButtonElement>('.aviso'));
    expect(items).toHaveLength(2);
    expect(items[1].textContent).toContain('Borrosa');

    items[0].click();
    await fixture.whenStable();

    expect(servicio.marcarLeida).toHaveBeenCalledWith('n1');
    expect(navigate).toHaveBeenCalledWith('/bancos/b1');
  });

  it('marks everything as read', async () => {
    await abrirMenu();

    (document.querySelector('.todas') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(servicio.marcarTodasLeidas).toHaveBeenCalled();
  });
});
