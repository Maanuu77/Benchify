#!/usr/bin/env node
// Escribe en frontend/src/environments/environment.prod.ts la dirección de la API y la configuración web
// de Firebase antes de compilar para producción, y añade el sitemap (lo sirve la API) a robots.txt. La
// usan el despliegue continuo (con las variables del repositorio) y el despliegue a mano. Estos valores
// no son secretos: acaban en el JavaScript que descarga el navegador.
//
//   API_URL=https://benchify-api.onrender.com FIREBASE_PROJECT_ID=mi-proyecto FIREBASE_API_KEY=AIza... \
//   FIREBASE_APP_ID=1:123:web:abc node deploy/configurar-frontend.mjs
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const requerida = (nombre) => {
  const valor = process.env[nombre]?.trim();
  if (!valor) {
    console.error(`Falta la variable ${nombre}`);
    process.exit(1);
  }
  return valor;
};

// Raíz pública del backend (sin /api/v1), p. ej. https://benchify-api.onrender.com
const api = requerida('API_URL').replace(/\/+$/, '').replace(/\/api\/v1$/, '');
const proyecto = requerida('FIREBASE_PROJECT_ID');
const config = {
  projectId: proyecto,
  apiKey: requerida('FIREBASE_API_KEY'),
  // El propio dominio de Hosting: el inicio de sesión con Google por redirección no depende así de
  // cookies de terceros (Hosting sirve el gestor de /__/auth en cualquiera de sus dominios)
  authDomain: process.env.FIREBASE_AUTH_DOMAIN?.trim() || `${proyecto}.web.app`,
  // Los proyectos nuevos usan <id>.firebasestorage.app; los antiguos, <id>.appspot.com
  storageBucket: process.env.FIREBASE_STORAGE_BUCKET?.trim() || `${proyecto}.firebasestorage.app`,
  appId: requerida('FIREBASE_APP_ID'),
};

const frontend = join(dirname(fileURLToPath(import.meta.url)), '..', 'frontend');
const texto = (v) => `'${v.replace(/\\/g, '\\\\').replace(/'/g, "\\'")}'`;

const ruta = join(frontend, 'src', 'environments', 'environment.prod.ts');
const original = readFileSync(ruta, 'utf8');
const bloque = `firebase: {\n${Object.entries(config).map(([k, v]) => `    ${k}: ${texto(v)},`).join('\n')}\n  },`;
const actualizado = original
  // Funciones como reemplazo: un "$" en los valores no se interpreta como patrón
  .replace(/apiUrl: '[^']*',/, () => `apiUrl: ${texto(`${api}/api/v1`)},`)
  .replace(/firebase: \{[\s\S]*?\n {2}\},/, () => bloque);
if (!actualizado.includes(bloque) || !actualizado.includes(`${api}/api/v1`)) {
  console.error(`No se han encontrado "apiUrl" y el bloque "firebase: { ... }" en ${ruta}`);
  process.exit(1);
}
writeFileSync(ruta, actualizado);

// El sitemap lo genera la API con los bancos publicados
const robots = join(frontend, 'public', 'robots.txt');
const reglas = readFileSync(robots, 'utf8').replace(/\n*Sitemap: .*\n?/g, '').trimEnd();
writeFileSync(robots, `${reglas}\n\nSitemap: ${api}/sitemap.xml\n`);

console.log(`API ${api} y proyecto de Firebase "${proyecto}" configurados para producción`);
