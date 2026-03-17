import { test, expect } from '@playwright/test';

const FRONTEND_URL = process.env.VITE_FRONTEND_URL || 'http://localhost:5173';

const uniqueEmail = () => `testuser${Date.now()}${Math.floor(Math.random() * 10000)}@test.com`;

async function loginAs(page, email, password, expectedUrlPattern) {
  await page.goto(`${FRONTEND_URL}/login`);
  await page.fill('input[name="email"]', email);
  await page.fill('input[name="password"]', password);
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL(expectedUrlPattern, { timeout: 15000 });
}

test.describe('Vaccination System E2E Tests', () => {
  test('1. User Registration Flow', async ({ page }) => {
    await page.goto(`${FRONTEND_URL}/register`);

    await page.fill('input[name="fullName"]', 'Test User');
    await page.fill('input[name="email"]', uniqueEmail());
    await page.fill('input[name="password"]', 'Test@123456');
    await page.fill('input[name="confirmPassword"]', 'Test@123456');
    await page.fill('input[name="age"]', '25');
    await page.click('button[type="submit"]');

    await expect(page.getByText('Registration Successful!')).toBeVisible();
    await expect(page.getByRole('link', { name: /Go to Login/i })).toBeVisible();
  });

  test('2. User Login Flow', async ({ page }) => {
    await loginAs(page, 'demo.user@vaccination.local', 'Demo@123', /\/user\/bookings/);
  });

  test('3. View Public Drives', async ({ page }) => {
    await page.goto(`${FRONTEND_URL}/drives`);
    await expect(page).toHaveURL(/\/drives/);
    await expect(page.getByRole('heading', { name: /Vaccination Drives/i })).toBeVisible();
  });

  test('4. View Public Centers', async ({ page }) => {
    await page.goto(`${FRONTEND_URL}/centers`);
    await expect(page).toHaveURL(/\/centers/);
    await expect(page.getByRole('heading', { name: /Vaccination Centers/i })).toBeVisible();
  });

  test('5. Login as Admin', async ({ page }) => {
    await loginAs(page, 'admin@vaccination.local', 'Admin@123', /\/admin\/dashboard/);
  });

  test('6. Admin Dashboard Access', async ({ page }) => {
    await loginAs(page, 'admin@vaccination.local', 'Admin@123', /\/admin\/dashboard/);
    await page.goto(`${FRONTEND_URL}/admin/dashboard`);
    await expect(page).toHaveURL(/\/admin\/dashboard/);
    await expect(page.getByRole('heading', { name: /Admin Dashboard/i })).toBeVisible();
  });

  test('7. Unauthorized Access to Admin', async ({ page }) => {
    await page.goto(`${FRONTEND_URL}/admin/dashboard`);
    await expect(page).toHaveURL(/\/login/);
  });

  test('8. Booking Flow', async ({ page }) => {
    await loginAs(page, 'demo.user@vaccination.local', 'Demo@123', /\/user\/bookings/);
    await page.goto(`${FRONTEND_URL}/drives`);
    await expect(page).toHaveURL(/\/drives/);
    await expect(page.getByRole('heading', { name: /Vaccination Drives/i })).toBeVisible();
  });

  test('9. User Bookings Page', async ({ page }) => {
    await loginAs(page, 'demo.user@vaccination.local', 'Demo@123', /\/user\/bookings/);
    await page.goto(`${FRONTEND_URL}/user/bookings`);
    await expect(page).toHaveURL(/\/user\/bookings/);
    await expect(page.getByRole('heading', { name: /My Bookings/i })).toBeVisible();
  });

  test('10. Logout Flow', async ({ page }) => {
    await loginAs(page, 'demo.user@vaccination.local', 'Demo@123', /\/user\/bookings/);
    await page.getByRole('button', { name: /My Account/i }).click();
    await page.getByRole('button', { name: /Logout/i }).click();
    await expect(page).toHaveURL(`${FRONTEND_URL}/`);
  });
});
