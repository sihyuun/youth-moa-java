import { expect, test } from '@playwright/test';
import { ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS, abortExternal, loginAdmin } from '../helpers';

/**
 * F4-admin-eligibility (Qn-1 A · 2026-09-09) RBAC 검증.
 * SYSTEM_ADMIN 만 허용, CENTER_ADMIN 은 403 (@PreAuthorize 클래스 레벨).
 */

const PROGRAM_ID = 7;

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('CENTER_ADMIN 은 편집 폼 진입 시 403', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    const response = await page.goto(`/admin/programs/${PROGRAM_ID}/eligibility`, {
        waitUntil: 'domcontentloaded',
    });
    expect(response?.status()).toBe(403);
});

test('CENTER_ADMIN 은 POST 저장 시 403', async ({ page }) => {
    await loginAdmin(page, ADMIN_CENTER1_EMAIL, ADMIN_SEED_PASS);
    await page.goto('/admin', { waitUntil: 'domcontentloaded' });
    const csrfToken = await page.locator('meta[name="_csrf"]').getAttribute('content') || '';
    const csrfHeader = await page.locator('meta[name="_csrf_header"]').getAttribute('content') || '';

    const response = await page.request.post(`/admin/programs/${PROGRAM_ID}/eligibility`, {
        form: { age: '차단 예정', region: '차단 예정', etc: '차단 예정' },
        headers: csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {},
    });
    expect(response.status()).toBe(403);
});

test('SYSTEM_ADMIN 은 편집 폼 진입 200', async ({ page }) => {
    await loginAdmin(page);
    const response = await page.goto(`/admin/programs/${PROGRAM_ID}/eligibility`, {
        waitUntil: 'domcontentloaded',
    });
    expect(response?.status()).toBe(200);
});
