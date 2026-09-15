import { expect, test } from '@playwright/test';
import { abortExternal, loginAdmin } from '../helpers';

/**
 * A5 admin-users (2026-09-15) — `/admin/users` 목록 기능 E2E.
 *
 * spec §6 기능 E2E 요구: 검색 · role filter · 페이지네이션 · role badge.
 * 계약(visual-admin-users) 은 정적 렌더 검증. 여기서는 실제 클릭·URL 변화·검색어 반영 실측.
 */

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('/admin/users 진입 → GNB "사용자 관리" 활성 + 페이지 타이틀 + role filter 4탭', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('nav.admin-header-nav a.admin-nav-link.active')).toHaveText('사용자 관리');
    await expect(page.locator('.admin-program-title')).toHaveText('사용자 관리');
    await expect(page.locator('nav.admin-program-tabs a.admin-program-tab')).toHaveCount(4);
});

test('시드 사용자 최소 10건 렌더 + role badge / status badge', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    const rows = page.locator('a.admin-user-row:not(.admin-user-row--head)');
    const count = await rows.count();
    expect(count).toBeGreaterThanOrEqual(10);
    // 최소 한 행 이상에 role badge · status badge 존재
    await expect(rows.first().locator('.admin-role-badge')).toBeVisible();
    await expect(rows.first().locator('.admin-user-status-badge')).toBeVisible();
});

test('role filter SYSTEM_ADMIN 클릭 → URL 반영 · 탭 활성 · 필터된 결과', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    await page.locator('nav.admin-program-tabs a').filter({ hasText: '시스템 관리자' }).click();
    await expect(page).toHaveURL(/role=SYSTEM_ADMIN/);
    await expect(
        page.locator('nav.admin-program-tabs a.admin-program-tab--active'),
    ).toHaveText('시스템 관리자');
    // 시스템 관리자 badge 만 노출 확인
    const badges = page.locator('a.admin-user-row .admin-role-badge');
    const badgeCount = await badges.count();
    expect(badgeCount).toBeGreaterThan(0);
    for (let i = 0; i < badgeCount; i++) {
        await expect(badges.nth(i)).toHaveText('시스템 관리자');
    }
});

test('검색어 입력 → URL q 파라미터 반영 + 검색어 유지', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    await page.locator('input.admin-program-search-input').fill('sysadmin');
    await page.locator('form.admin-program-filters button[type="submit"]').click();
    await expect(page).toHaveURL(/q=sysadmin/);
    await expect(page.locator('input.admin-program-search-input')).toHaveValue('sysadmin');
    // 결과에 sysadmin 이메일 노출
    await expect(page.locator('a.admin-user-row').first()).toContainText('sysadmin');
});

test('존재하지 않는 검색어 → empty state 노출', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/users?q=___NO_MATCH_XYZ___', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.admin-user-empty')).toContainText('검색 결과가 없어요');
});

test('페이지네이션 2페이지 이동 → page=1 반영', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    const paging = page.locator('nav.admin-notice-paging');
    if ((await paging.count()) === 0) {
        // 시드 부족 시 스킵
        return;
    }
    await paging.locator('a.admin-page-btn').filter({ hasText: '2' }).first().click();
    await expect(page).toHaveURL(/page=1/);
    await expect(
        page.locator('nav.admin-notice-paging .admin-page-btn--active'),
    ).toHaveText('2');
});

test('사용자 행 클릭 → 상세 페이지 이동', async ({ page }) => {
    await loginAdmin(page);
    await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
    const firstRow = page.locator('a.admin-user-row:not(.admin-user-row--head)').first();
    await firstRow.click();
    await expect(page).toHaveURL(/\/admin\/users\/\d+/);
    await expect(page.locator('.admin-user-detail-grid')).toBeVisible();
});
