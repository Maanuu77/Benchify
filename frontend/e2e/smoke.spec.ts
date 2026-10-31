import { expect, test } from '@playwright/test';
import { mockApi } from './api-mock';

test.describe('anonymous visitor', () => {
  test('sees the benches of the map area, best rated first', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');

    await expect(page).toHaveURL(/\/mapa$/);
    await expect(page.getByRole('heading', { name: '2 bancos en esta zona' })).toBeVisible();
    const lista = page.getByRole('navigation', { name: 'Bancos mejor valorados de la zona' })
      .or(page.getByLabel('Bancos mejor valorados de la zona'));
    await expect(lista.getByRole('link').first()).toContainText('Torre del Oro');
    await expect(page.getByRole('region', { name: 'Mapa de bancos' })).toBeVisible();
  });

  test('filters the map and sends the filter to the API', async ({ page }) => {
    const peticiones = await mockApi(page);
    await page.goto('/mapa');
    await expect(page.getByRole('heading', { name: '2 bancos en esta zona' })).toBeVisible();

    await page.getByRole('button', { name: 'Accesible', pressed: false }).click();

    await expect(page.getByRole('heading', { name: '1 banco en esta zona' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Accesible', pressed: true })).toBeVisible();
    // El filtro queda en la URL para poder volver o compartir el enlace
    await expect(page).toHaveURL(/accesible=1/);
    expect(peticiones.some((p) => p.startsWith('/api/v1/bancos?') && p.includes('accesible=true'))).toBe(true);
  });

  test('opens a bench page with its reviews', async ({ page }) => {
    await mockApi(page);
    await page.goto('/bancos/b-torre');

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Banco frente a la Torre del Oro');
    await expect(page).toHaveTitle(/Torre del Oro/);
    await expect(page.getByText('3 check-ins')).toBeVisible();
    await expect(page.getByText('Las mejores vistas del río.')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Inicia sesión' })).toBeVisible();
  });

  test('is sent to login when saving a favourite and comes back afterwards', async ({ page }) => {
    await mockApi(page);
    await page.goto('/bancos/b-torre');

    await page.getByRole('button', { name: 'Guardar' }).click();

    await expect(page).toHaveURL(/\/login\?returnUrl=%2Fbancos%2Fb-torre$/);
    await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  });

  test('gets a friendly page for unknown benches and routes', async ({ page }) => {
    await mockApi(page);
    await page.goto('/bancos/no-existe');
    await expect(page.getByRole('heading', { name: 'Banco no encontrado' })).toBeVisible();

    await page.goto('/esto-no-existe');
    await expect(page.getByRole('heading', { name: 'Página no encontrada' })).toBeVisible();
  });

  test('cannot open protected pages without signing in', async ({ page }) => {
    await mockApi(page);
    await page.goto('/bancos/nuevo');
    await expect(page).toHaveURL(/\/login\?returnUrl=%2Fbancos%2Fnuevo$/);

    await page.goto('/admin');
    await expect(page).toHaveURL(/\/login\?returnUrl=%2Fadmin$/);
  });
});

test.describe('forms and accessibility', () => {
  test('login validates the form before contacting Firebase', async ({ page }) => {
    await mockApi(page);
    await page.goto('/login');

    await page.getByLabel('Email').fill('no-es-un-email');
    await page.getByRole('button', { name: 'Entrar', exact: true }).click();

    await expect(page.getByText('Introduce un email válido')).toBeVisible();
    await expect(page.getByText('La contraseña es obligatoria')).toBeVisible();
  });

  test('the first Tab reaches the skip link, which moves focus to the content', async ({ page }) => {
    await mockApi(page);
    await page.goto('/login');

    await page.keyboard.press('Tab');
    const skip = page.getByRole('link', { name: 'Saltar al contenido' });
    await expect(skip).toBeFocused();
    await skip.press('Enter');
    await expect(page).toHaveURL(/#contenido$/);
  });
});

test.describe('mobile layout', () => {
  test('navigation lives in a bottom bar and the map has no header', async ({ page, isMobile }) => {
    test.skip(!isMobile, 'Solo en el proyecto móvil');
    await mockApi(page);
    await page.goto('/mapa');

    const barra = page.getByRole('navigation', { name: 'Principal' });
    await expect(barra.getByRole('link', { name: 'Mapa' })).toBeVisible();
    await expect(barra.getByRole('link', { name: 'Entrar' })).toBeVisible();
    await expect(page.getByRole('banner')).toHaveCount(0);

    await barra.getByRole('link', { name: 'Entrar' }).click();
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByRole('banner')).toBeVisible();
  });
});
