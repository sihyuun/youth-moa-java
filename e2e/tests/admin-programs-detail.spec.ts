import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('/admin/programs/1 진입 → 상세 카드 + 하위 관리 링크', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs/1', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.admin-program-detail-title')).toBeVisible();
    await expect(page.locator('a[href*="/eligibility"]')).toBeVisible();
    await expect(page.locator('a[href*="/dynamic-fields"]')).toBeVisible();
});

test('편집·삭제 버튼 disabled (A3 이월)', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs/1', { waitUntil: 'domcontentloaded' });
    const disabledButtons = page.locator('.admin-program-detail-actions button[disabled]');
    expect(await disabledButtons.count()).toBeGreaterThanOrEqual(2);
});

test('자격요건 편집 링크 클릭 → F4 페이지 이동', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs/1', { waitUntil: 'domcontentloaded' });
    await Promise.all([
        page.waitForURL(/\/admin\/programs\/1\/eligibility/),
        page.locator('a[href$="/eligibility"]').click(),
    ]);
});

test('목록 → 상세 이동', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });
    const firstNameLink = page.locator('.admin-program-name-link').first();
    await Promise.all([
        page.waitForURL(/\/admin\/programs\/\d+$/),
        firstNameLink.click(),
    ]);
    await expect(page.locator('.admin-program-detail-title')).toBeVisible();
});
