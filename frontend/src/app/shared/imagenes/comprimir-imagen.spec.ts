import { comprimirImagen, LADO_MAXIMO } from './comprimir-imagen';

describe('comprimirImagen', () => {
  const foto = (bytes: number) => new File([new Uint8Array(bytes)], 'IMG_0001.jpg', { type: 'image/jpeg' });
  let drawImage: ReturnType<typeof vi.fn>;
  let tipoGenerado: string;

  beforeEach(() => {
    drawImage = vi.fn();
    tipoGenerado = 'image/webp';
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({ drawImage } as never);
    vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation(function (callback, tipo) {
      callback(tipo === tipoGenerado ? new Blob([new Uint8Array(1000)], { type: tipoGenerado }) : null);
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  function bitmap(width: number, height: number) {
    const close = vi.fn();
    vi.stubGlobal('createImageBitmap', vi.fn(async () => ({ width, height, close })));
    return close;
  }

  it('shrinks a large phone photo to WebP keeping its aspect ratio and orientation', async () => {
    const close = bitmap(4032, 3024);

    const resultado = await comprimirImagen(foto(6_000_000));

    expect(createImageBitmap).toHaveBeenCalledWith(expect.any(File), { imageOrientation: 'from-image' });
    expect(drawImage).toHaveBeenCalledWith(expect.anything(), 0, 0, LADO_MAXIMO, 1200);
    expect(resultado.type).toBe('image/webp');
    expect(resultado.name).toBe('IMG_0001.webp');
    expect(resultado.size).toBe(1000);
    expect(close).toHaveBeenCalled();
  });

  it('falls back to JPEG when the browser cannot encode WebP', async () => {
    bitmap(3000, 2000);
    tipoGenerado = 'image/jpeg';

    const resultado = await comprimirImagen(foto(3_000_000));

    expect(resultado.type).toBe('image/jpeg');
    expect(resultado.name).toBe('IMG_0001.jpg');
  });

  it('keeps small photos and undecodable files untouched', async () => {
    bitmap(800, 600);
    const pequena = foto(200_000);
    expect(await comprimirImagen(pequena)).toBe(pequena);

    vi.stubGlobal('createImageBitmap', vi.fn(async () => Promise.reject(new Error('formato'))));
    const rara = foto(6_000_000);
    expect(await comprimirImagen(rara)).toBe(rara);
  });
});
