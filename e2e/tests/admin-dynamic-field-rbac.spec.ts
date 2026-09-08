import { expect, test } from '@playwright/test';
import {
    ADMIN_CENTER1_EMAIL,
    ADMIN_SEED_PASS,
    abortExternal,
    loginAdmin,
} from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('CENTER_ADMIN 은 목록 진입 시 403 (Qn-1 A SYSTEM only)', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    const response = await page.goto('/admin/programs/7/dynamic-fields', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(403);
});

test('CENTER_ADMIN 은 신규 폼 진입 시 403', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    const response = await page.goto('/admin/programs/7/dynamic-fields/new', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(403);
});

test('CENTER_ADMIN 은 POST 등록 시 403', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    // CSRF 토큰 확보용 → 다른 admin 페이지 열어서 세션 쿠키 확보만 (POST 는 실패 예상)
    await page.goto('/admin', { waitUntil: 'domcontentloaded' });
    const csrfToken = await page.locator('meta[name="_csrf"]').getAttribute('content') || '';
    const csrfHeader = await page.locator('meta[name="_csrf_header"]').getAttribute('content') || '';

    const response = await page.request.post('/admin/programs/7/dynamic-fields', {
        form: {
            fieldType: 'TEXT',
            label: '차단 예정',
            sortOrder: '99',
            maxLength: '100',
        },
        headers: csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {},
    });
    expect(response.status()).toBe(403);
});

test('SYSTEM_ADMIN 은 신규 폼 진입 200', async ({ page }) => {
    await loginAdmin(page);
    const response = await page.goto('/admin/programs/7/dynamic-fields/new', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(200);
});
