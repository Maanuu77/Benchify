import { defineConfig, devices } from '@playwright/test';

/**
 * - `npm run e2e`: proyectos desktop y mobile, con la API simulada (no necesitan backend ni emuladores).
 * - `npm run e2e:stack`: proyecto stack, contra el backend y Firebase Emulator Suite reales.
 * - PW_CHANNEL=msedge (o chrome): usa un navegador ya instalado en lugar del Chromium de Playwright.
 */
const channel = process.env['PW_CHANNEL'];
const ci = !!process.env['CI'];

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: ci,
  retries: ci ? 1 : 0,
  reporter: ci ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:4200',
    locale: 'es-ES',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], channel }, testIgnore: /stack\.spec\.ts/ },
    { name: 'mobile', use: { ...devices['Pixel 7'], channel }, testIgnore: /stack\.spec\.ts/ },
    { name: 'stack', use: { ...devices['Desktop Chrome'], channel }, testMatch: /stack\.spec\.ts/ },
  ],
  webServer: {
    command: 'npm run start -- --port 4200',
    url: 'http://localhost:4200',
    reuseExistingServer: !ci,
    timeout: 180_000,
  },
});
