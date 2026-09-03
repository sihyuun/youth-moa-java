import { expect, test } from '@playwright/test';
import { abortExternal, ADMIN_SYSTEM_EMAIL, loginAdmin, SEED_PASS, seedEmail } from '../helpers';

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
});

test('대시보드 스탯 카드 4개 + 실제 count 렌더', async ({ page }) => {
    await loginAdmin(page);
    await expect(page.locator('.admin-stat-card')).toHaveCount(4);
    // 각 카드 라벨
    await expect(page.locator('.admin-stat-card').nth(0).locator('.admin-stat-card-label')).toHaveText('진행중 프로그램');
    await expect(page.locator('.admin-stat-card').nth(3).locator('.admin-stat-card-label')).toHaveText('전체 회원');
    // count 는 정수 텍스트 (음수/문자 없음)
    for (let i = 0; i < 4; i++) {
        const value = await page.locator('.admin-stat-card').nth(i).locator('.admin-stat-card-value').innerText();
        expect(value).toMatch(/^\d+$/);
    }
    // 승인 대기 count 도 정수
    const pending = await page.locator('.admin-pending-card-value').innerText();
    expect(pending).toMatch(/^\d+$/);
});

test('유저 드롭다운 → 사용자 페이지 클릭 → 세션 유지된 채 / 이동', async ({ page }) => {
    await loginAdmin(page);
    await page.locator('.admin-header-user-trigger').click();
    await expect(page.locator('#admin-user-dropdown')).toBeVisible();
    await page.locator('#admin-user-dropdown a[href="/"]').click();
    await page.waitForURL('/');
    // 세션 유지 확인 — 사용자 헤더의 인증 사용자 이름 표시 요소가 노출됨
    await expect(page.locator('.header-user-name')).toBeVisible();
});

test('유저 드롭다운 → 로그아웃 → /admin/login?logout 도달 + alert', async ({ page }) => {
    await loginAdmin(page);
    await page.locator('.admin-header-user-trigger').click();
    await expect(page.locator('#admin-user-dropdown')).toBeVisible();
    await page.locator('.admin-header-logout-form button[type="submit"]').click();
    await page.waitForURL(/\/admin\/login\?logout/);
    await expect(page.locator('.admin-auth-alert--success')).toContainText('로그아웃되었습니다.');
});

test('사용자 헤더 드롭다운에 관리자 페이지 링크 (관리자만 노출)', async ({ page }) => {
    // 관리자로 사용자 페이지 로그인 상태 만들기 — sysadmin 은 사용자 formLogin 으로도 인증 가능.
    // 다만 A1 은 admin filter 가 /admin/** 만 매칭하므로 /login 은 user chain 이 담당.
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.locator('input[name="username"]').fill(ADMIN_SYSTEM_EMAIL);
    await page.locator('input[name="password"]').fill('Admin!234');
    await page.locator('form.auth-form-prototype button[type="submit"]').click();
    await page.waitForURL('/');
    await page.locator('.header-user-trigger').click();
    await expect(page.locator('.header-user-dropdown a[href="/admin"]')).toContainText('관리자 페이지');
});

test('USER 계정은 사용자 헤더 드롭다운에 관리자 페이지 링크 미노출', async ({ page }) => {
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.locator('input[name="username"]').fill(seedEmail(1));
    await page.locator('input[name="password"]').fill(SEED_PASS);
    await page.locator('form.auth-form-prototype button[type="submit"]').click();
    await page.waitForURL('/');
    await page.locator('.header-user-trigger').click();
    await expect(page.locator('.header-user-dropdown a[href="/admin"]')).toHaveCount(0);
});
