import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BancosApi } from '../../../core/api/bancos-api';
import { Imagen } from '../../../core/api/models';
import { AuthService } from '../../../core/auth/auth.service';
import { FakeAuthService } from '../../../core/auth/testing/fake-auth.service';
import { Notifier } from '../../../core/ui/notifier';
import { Confirmation } from '../../../shared/ui/confirm-dialog';
import { GaleriaImagenes } from './galeria-imagenes';

const imagen = (id: string, extra: Partial<Imagen> = {}): Imagen => ({
  id, bancoId: 'b1', url: `https://storage.test/${id}.jpg`, thumbUrl: `https://storage.test/${id}_thumb.jpg`,
  requiereAutenticacion: false, estado: 'APROBADA', subidaPor: 'luis',
  fechaSubida: '2026-10-01T10:00:00Z', ...extra,
});

describe('GaleriaImagenes', () => {
  let fixture: ComponentFixture<GaleriaImagenes>;
  let element: HTMLElement;
  let auth: FakeAuthService;
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let notifier: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    auth = new FakeAuthService();
    auth.signIn({ uid: 'ana' });
    api = {
      imagenes: vi.fn(() => of([imagen('i1'), imagen('i2', {
        estado: 'PENDIENTE', subidaPor: 'ana', requiereAutenticacion: true,
        url: 'http://api.test/api/v1/imagenes/i2/archivo', thumbUrl: 'http://api.test/api/v1/imagenes/i2/miniatura',
      })])),
      subirImagen: vi.fn(() => of(imagen('i3', { estado: 'PENDIENTE' }))),
      eliminarImagen: vi.fn(() => of(undefined)),
    };
    notifier = { success: vi.fn(), error: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: BancosApi, useValue: api },
        { provide: Notifier, useValue: notifier },
        { provide: Confirmation, useValue: { ask: vi.fn(async () => true) } },
      ],
    });
    fixture = TestBed.createComponent(GaleriaImagenes);
    fixture.componentRef.setInput('bancoId', 'b1');
    fixture.componentRef.setInput('tituloBanco', 'Glorieta');
    fixture.componentRef.setInput('puedeSubir', true);
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('labels pending photos and lets the uploader delete them', () => {
    expect(element.querySelectorAll('img')).toHaveLength(2);
    expect(element.querySelector('img')?.getAttribute('alt')).toBe('Foto 1 de Glorieta');
    expect(element.textContent).toContain('Pendiente de revisión');
    expect(element.querySelectorAll('button[aria-label="Borrar foto"]')).toHaveLength(1);
  });

  it('shows thumbnails and downloads pending photos through the API', async () => {
    const http = TestBed.inject(HttpTestingController);
    const [publica, pendiente] = Array.from(element.querySelectorAll('img'));
    expect(publica.getAttribute('src')).toBe('https://storage.test/i1_thumb.jpg');
    expect(pendiente.hasAttribute('src')).toBe(false);

    const blobUrl = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:pendiente');
    const request = http.expectOne('http://api.test/api/v1/imagenes/i2/miniatura');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['jpg']));

    expect(blobUrl).toHaveBeenCalled();
    expect(pendiente.getAttribute('src')).toBe('blob:pendiente');
    blobUrl.mockRestore();
  });

  it('does not reload the photos when only the token is renewed', async () => {
    TestBed.inject(HttpTestingController).match(() => true);
    auth.signIn({ uid: 'ana', displayName: 'Ana renovada' });
    await fixture.whenStable();

    expect(api['imagenes']).toHaveBeenCalledTimes(1);
  });

  it('replaces a photo that fails to load with an accessible placeholder', async () => {
    element.querySelector('img')!.dispatchEvent(new Event('error'));
    await fixture.whenStable();

    expect(element.querySelectorAll('img')).toHaveLength(1);
    expect(element.querySelector('.rota')?.getAttribute('aria-label')).toBe('Foto 1 no disponible');
  });

  it('rejects invalid files before uploading and uploads valid ones', async () => {
    const input = element.querySelector<HTMLInputElement>('input[type=file]')!;
    const elegir = (file: File) => {
      Object.defineProperty(input, 'files', { value: [file], configurable: true });
      input.dispatchEvent(new Event('change'));
      return fixture.whenStable();
    };

    await elegir(new File(['gif'], 'a.gif', { type: 'image/gif' }));
    expect(api['subirImagen']).not.toHaveBeenCalled();
    expect(notifier.error).toHaveBeenCalledWith('Solo se admiten fotos JPEG, PNG o WebP.');

    await elegir(new File([new Uint8Array([0xff, 0xd8, 0xff])], 'a.jpg', { type: 'image/jpeg' }));
    expect(api['subirImagen']).toHaveBeenCalled();
    expect(notifier.success).toHaveBeenCalledWith('Foto enviada: se publicará cuando la revisemos');
  });

  it('deletes an own photo after confirmation', async () => {
    element.querySelector<HTMLButtonElement>('button[aria-label="Borrar foto"]')!.click();
    await fixture.whenStable();

    expect(api['eliminarImagen']).toHaveBeenCalledWith('i2');
    expect(element.querySelectorAll('img')).toHaveLength(1);
  });
});
