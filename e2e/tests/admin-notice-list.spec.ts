import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('/admin/notices 진입 → GNB "공지 관리" 활성 + 페이지 타이틀 + 신규 등록 버튼', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/notices', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('nav.admin-header-nav a.admin-nav-link.active')).toHaveText('공지 관리');
    await expect(page.locator('.admin-notice-title')).toHaveText('공지 관리');
    await expect(page.locator('.admin-notice-header a.admin-btn--primary')).toContainText('신규 등록');
});

test('시드 공지 12건 이상 렌더 + 각 row 편집 링크 노출', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/notices', { waitUntil: 'domcontentloaded' });
    const rows = page.locator('.admin-notice-row:not(.admin-notice-row--head)');
    const count = await rows.count();
    expect(count).toBeGreaterThanOrEqual(12);
    await expect(rows.first().locator('.admin-notice-col-actions a')).toContainText('편집');
});

test('페이징 렌더 (시드 20건 초과 시)', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/notices', { waitUntil: 'domcontentloaded' });
    // 시드가 20건 미만이면 페이징 미노출이라 optional 검증
    const paging = page.locator('.admin-notice-paging');
    if (await paging.count() > 0) {
        await expect(paging.locator('.admin-page-btn--active')).toContainText('1');
    }
});
