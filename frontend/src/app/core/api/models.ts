/** Tipos de la API REST v1 (reflejan los DTOs del backend). */

export type Rol = 'USER' | 'ADMIN';
export type EstadoBanco = 'PENDIENTE' | 'PUBLICADO' | 'OCULTO' | 'ELIMINADO';
export type EstadoImagen = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';
export type MaterialBanco = 'MADERA' | 'METAL' | 'PIEDRA' | 'HORMIGON' | 'PLASTICO' | 'OTRO';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PageRequest {
  page?: number;
  size?: number;
}

export interface Etiqueta {
  id: number;
  nombre: string;
  color: string;
  icono: string;
}

export interface BancoResumen {
  id: string;
  titulo: string;
  lat: number;
  lng: number;
  ciudad: string | null;
  estado: EstadoBanco;
  puntuacionMedia: number;
  numResenas: number;
  accesible: boolean;
  conSombra: boolean;
  conRespaldo: boolean;
  etiquetas: string[];
}

export interface Banco {
  id: string;
  titulo: string;
  descripcion: string | null;
  lat: number;
  lng: number;
  direccion: string | null;
  ciudad: string | null;
  provincia: string | null;
  pais: string;
  estado: EstadoBanco;
  conSombra: boolean;
  accesible: boolean;
  conRespaldo: boolean;
  material: MaterialBanco | null;
  puntuacionMedia: number;
  numResenas: number;
  autor: { id: string; nombre: string | null };
  etiquetas: Etiqueta[];
  fechaSubida: string;
  fechaVerificacion: string | null;
}

export interface BancoCercano {
  banco: BancoResumen;
  distanciaMetros: number;
}

export interface BancoRequest {
  titulo: string;
  descripcion?: string | null;
  lat: number;
  lng: number;
  direccion?: string | null;
  ciudad?: string | null;
  provincia?: string | null;
  pais?: string | null;
  conSombra: boolean;
  accesible: boolean;
  conRespaldo: boolean;
  material?: MaterialBanco | null;
  etiquetaIds: number[];
}

export interface FiltroBancos {
  accesible?: boolean;
  conSombra?: boolean;
  conRespaldo?: boolean;
  etiqueta?: number;
  puntuacionMin?: number;
}

export interface BoundingBox {
  minLat: number;
  minLng: number;
  maxLat: number;
  maxLng: number;
}

export interface Imagen {
  id: string;
  bancoId: string;
  url: string;
  estado: EstadoImagen;
  subidaPor: string;
  fechaSubida: string;
}

export interface Resena {
  id: string;
  bancoId: string;
  autorId: string;
  autorNombre: string | null;
  puntuacion: number;
  comentario: string | null;
  fecha: string;
  fechaEdicion: string | null;
}

export interface ResenaRequest {
  puntuacion: number;
  comentario?: string | null;
}

export interface Favorito {
  bancoId: string;
  titulo: string;
  lat: number;
  lng: number;
  ciudad: string | null;
  puntuacionMedia: number;
  numResenas: number;
  fecha: string;
}

export interface CheckIn {
  id: string;
  bancoId: string;
  tituloBanco: string | null;
  distanciaMetros: number;
  fecha: string;
}

export interface Usuario {
  id: string;
  email: string | null;
  nombre: string;
  avatarUrl: string | null;
  ciudad: string | null;
  rol: Rol;
  fechaRegistro: string;
}

export interface PerfilPublico {
  id: string;
  nombre: string;
  avatarUrl: string | null;
  ciudad: string | null;
  fechaRegistro: string;
}

export interface EstadisticasUsuario {
  bancosPublicados: number;
  resenas: number;
  favoritos: number;
  checkIns: number;
}

export interface EstadisticasModeracion {
  bancosPendientes: number;
  bancosPublicados: number;
  bancosOcultos: number;
  resenasReportadas: number;
  imagenesPendientes: number;
  totalResenas: number;
  totalUsuarios: number;
}

export interface BancoModeracion {
  id: string;
  titulo: string;
  descripcion: string | null;
  lat: number;
  lng: number;
  direccion: string | null;
  ciudad: string | null;
  estado: EstadoBanco;
  autorId: string;
  autorNombre: string | null;
  etiquetas: string[];
  fechaSubida: string;
}

export interface ResenaModeracion {
  id: string;
  bancoId: string;
  autorId: string;
  autorNombre: string | null;
  puntuacion: number;
  comentario: string | null;
  numReportes: number;
  fecha: string;
}

export interface ImagenModeracion {
  id: string;
  bancoId: string;
  url: string;
  estado: EstadoImagen;
  subidaPor: string;
  tamanoBytes: number;
  fechaSubida: string;
}
