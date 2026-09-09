import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('/admin/programs 진입 → GNB "프로그램 관리" 활성 + 페이지 타이틀', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('nav.admin-header-nav a.admin-nav-link.active')).toHaveText('프로그램 관리');
    await expect(page.locator('.admin-program-title')).toHaveText('프로그램 관리');
});

test('시드 프로그램 5건 이상 렌더 + 편집 링크', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });
    const rows = page.locator('.admin-program-row:not(.admin-program-row--head)');
    const count = await rows.count();
    expect(count).toBeGreaterThanOrEqual(5);
    await expect(rows.first().locator('.admin-program-col-actions a')).toContainText('편집');
});

test('필터 탭 5종 존재 + OPEN 탭 클릭 → URL 변경', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });
    const tabs = page.locator('.admin-program-tabs .admin-program-tab');
    await expect(tabs).toHaveCount(5);
    await page.locator('.admin-program-tabs a', { hasText: '모집중' }).click();
    await page.waitForURL(/status=OPEN/);
    await expect(page.locator('.admin-program-tab--active')).toContainText('모집중');
});

test('검색 → URL 에 q 반영', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/programs', { waitUntil: 'domcontentloaded' });
    await page.locator('.admin-program-search-input').fill('청년');
    await page.locator('.admin-program-search button[type="submit"]').click();
    await page.waitForURL(/q=%EC%B2%AD%EB%85%84|q=청년/);
});
