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

test('CENTER_ADMIN 은 신규 폼 진입 시 403 (Qn-1 B UD only)', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    const response = await page.goto('/admin/terms/new', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(403);
});

test('CENTER_ADMIN 은 편집 저장 POST 시 403', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);

    // CSRF 토큰 확보용으로 편집 화면 진입 (조회는 허용)
    await page.goto('/admin/terms/1', { waitUntil: 'domcontentloaded' });
    const csrfToken = await page.locator('meta[name="_csrf"]').getAttribute('content') || '';
    const csrfHeader = await page.locator('meta[name="_csrf_header"]').getAttribute('content') || '';

    const response = await page.request.post('/admin/terms/1', {
        form: {
            title: '변경 시도',
            contentPath: '/terms',
            content: '<p>x</p>',
            required: 'true',
            sortOrder: '1',
            isActive: 'true',
        },
        headers: csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {},
    });
    expect(response.status()).toBe(403);
});

test('CENTER_ADMIN 은 삭제 POST 시 403', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    await page.goto('/admin/terms/1', { waitUntil: 'domcontentloaded' });
    const csrfToken = await page.locator('meta[name="_csrf"]').getAttribute('content') || '';
    const csrfHeader = await page.locator('meta[name="_csrf_header"]').getAttribute('content') || '';

    const response = await page.request.post('/admin/terms/1/delete', {
        headers: csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {},
    });
    expect(response.status()).toBe(403);
});

test('SYSTEM_ADMIN 은 신규 폼 진입 200 (게이트 통과)', async ({ page }) => {
    await loginAdmin(page);
    const response = await page.goto('/admin/terms/new', { waitUntil: 'domcontentloaded' });
    expect(response?.status()).toBe(200);
});
