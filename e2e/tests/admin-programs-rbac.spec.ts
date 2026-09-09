import { expect, test } from '@playwright/test';
import {
    abortExternal,
    ADMIN_CENTER1_EMAIL,
    ADMIN_SEED_PASS,
    loginAdmin,
    SEED_PASS,
    seedEmail,
} from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('USER 계정으로 /admin/programs 접근 → 403', async ({ page }) => {
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.locator('input[name="username"]').fill(seedEmail(1));
    await page.locator('input[name="password"]').fill(SEED_PASS);
    await page.locator('form.auth-form-prototype button[type="submit"]').click();
    await page.waitForURL('/');
    const response = await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(403);
});

test('SYSTEM_ADMIN 은 목록·상세 모두 200', async ({ page }) => {
    await loginAdmin(page);
    let response = await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(200);
    response = await page.goto('/admin/programs/1', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(200);
});

test('CENTER_ADMIN 은 목록 접근 200 (자기 센터 프로그램만 노출)', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    const response = await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(200);
    // 목록이 렌더돼야 함 (자기 센터 프로그램 0건이라도 페이지 자체는 200)
    await expect(page.locator('.admin-program-title')).toHaveText('프로그램 관리');
});
