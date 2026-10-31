/** Lado mayor con el que se suben las fotos. El backend las vuelve a recodificar a 2048 px como mucho. */
export const LADO_MAXIMO = 1600;
/** Por debajo de este tamaño y de LADO_MAXIMO la foto se sube tal cual. */
const BYTES_SIN_COMPRIMIR = 1024 * 1024;
const CALIDAD = 0.85;

/**
 * Reduce una foto antes de subirla: lado mayor de LADO_MAXIMO px y WebP (JPEG si el navegador no
 * sabe generar WebP), respetando la orientación EXIF. Una foto de móvil de 6–8 MB queda en unos
 * cientos de KB, así que se sube mucho antes y no choca con el límite del servidor. Si el navegador
 * no puede decodificarla, se devuelve la original y el servidor decide.
 */
export async function comprimirImagen(file: File): Promise<File> {
  if (typeof createImageBitmap !== 'function') {
    return file;
  }
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  } catch {
    return file;
  }
  try {
    const escala = Math.min(1, LADO_MAXIMO / Math.max(bitmap.width, bitmap.height));
    if (escala === 1 && file.size <= BYTES_SIN_COMPRIMIR) {
      return file;
    }
    const ancho = Math.round(bitmap.width * escala);
    const alto = Math.round(bitmap.height * escala);
    const canvas = document.createElement('canvas');
    canvas.width = ancho;
    canvas.height = alto;
    const contexto = canvas.getContext('2d');
    if (!contexto) {
      return file;
    }
    contexto.drawImage(bitmap, 0, 0, ancho, alto);
    const blob = (await aBlob(canvas, 'image/webp')) ?? (await aBlob(canvas, 'image/jpeg'));
    if (!blob || blob.size >= file.size) {
      return file;
    }
    const extension = blob.type === 'image/webp' ? 'webp' : 'jpg';
    const nombre = file.name.replace(/\.[^.]+$/, '') + '.' + extension;
    return new File([blob], nombre, { type: blob.type, lastModified: file.lastModified });
  } finally {
    bitmap.close();
  }
}

/** toBlob devuelve PNG si no soporta el tipo pedido: en ese caso se trata como no soportado. */
function aBlob(canvas: HTMLCanvasElement, tipo: string): Promise<Blob | null> {
  return new Promise((resolve) =>
    canvas.toBlob((blob) => resolve(blob && blob.type === tipo ? blob : null), tipo, CALIDAD),
  );
}
