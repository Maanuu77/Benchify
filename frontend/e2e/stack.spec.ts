import { expect, Page, test } from '@playwright/test';

/**
 * Contra el stack real (backend en :8080 con perfil dev y Firebase Emulator Suite).
 * Usa los usuarios de prueba que crea el backend al arrancar (DevUsersSeeder) con la contraseña
 * de SEED_USERS_PASSWORD. Solo lee datos, así que se puede repetir sin limpiar la base de datos.
 */
const PASSWORD = process.env['SEED_USERS_PASSWORD'] ?? 'benchify-dev';

async function login(page: Page, email: string): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Contraseña').fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page).toHaveURL(/\/mapa$/);
}

test('@stack a user signs in with the Auth emulator and sees their profile', async ({ page }) => {
  await login(page, 'ana@benchify.dev');

  await expect(page.getByRole('link', { name: 'Añadir banco' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Moderación' })).toHaveCount(0);

  await page.goto('/perfil');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Ana García');
});

test('@stack the seeded map shows real benches from PostGIS', async ({ page }) => {
  await page.goto('/mapa');

  await expect(page.getByRole('heading', { name: /bancos en esta zona/ })).not.toHaveText('0 bancos en esta zona');
  await page.getByRole('link', { name: /Torre del Oro/ }).first().click();
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Torre del Oro');
});

test('@stack an admin reaches the moderation panel', async ({ page }) => {
  await login(page, 'admin@benchify.dev');

  await page.getByRole('link', { name: 'Moderación' }).click();
  await expect(page.getByRole('heading', { name: 'Moderación' })).toBeVisible();
  await expect(page.getByText('bancos pendientes')).toBeVisible();
});
