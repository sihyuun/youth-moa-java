import { test, expect, type Page } from '@playwright/test';

/**
 * 헤더 fragment — 인증 상태별 UI 분기.
 *
 * fragments/header.html:
 *  - 공통: 로고, nav 3개(프로그램·청년센터·공지사항), 검색 아이콘
 *  - 비인증: .header-icon-btn--primary (로그인 아이콘)
 *  - 인증: .header-user-menu (아바타 + 이름 + 드롭다운 마이페이지/로그아웃)
 *
 * currentPage 하이라이트: nav-link.active — 프로그램 목록 진입 시 "프로그램" 이 active
 */

const SEED_USER = 'seed30@youth-moa.test';
const SEED_PASS = 'Test1234!';

async function login(page: Page) {
    await page.goto('/login', { waitUntil: 'commit' });
    await page.locator('input[name="username"]').fill(SEED_USER);
    await page.locator('input[name="password"]').fill(SEED_PASS);
    await page.locator('form.auth-form-prototype button[type="submit"]').click();
    await page.waitForURL('/');
}

test.beforeEach(async ({ page }) => {
    await page.route(/^https?:\/\/(?!localhost)/, route => route.abort());
});

test('비로그인 헤더: 로그인 아이콘 노출, 사용자 메뉴 없음', async ({ page }) => {
    await page.goto('/', { waitUntil: 'commit' });
    // 공통 nav 3개
    const navLinks = page.locator('.header-nav .nav-link');
    await expect(navLinks).toHaveCount(3);
    await expect(navLinks.nth(0)).toContainText('프로그램');
    await expect(navLinks.nth(1)).toContainText('청년센터');
    await expect(navLinks.nth(2)).toContainText('공지사항');
    // 비인증 우측: 로그인 아이콘
    await expect(page.locator('a.header-icon-btn--primary[href="/login"]')).toBeVisible();
    // 사용자 메뉴는 없어야 함
    await expect(page.locator('.header-user-menu')).toHaveCount(0);
});

test('로그인 후 헤더: 사용자 이름 + 드롭다운(마이페이지·로그아웃) 노출', async ({ page }) => {
    await login(page);
    // 홈에서 확인
    await expect(page.locator('.header-user-menu')).toBeVisible();
    await expect(page.locator('.header-user-name')).toContainText('시드유저30');
    // 아바타는 첫 글자 ("시")
    await expect(page.locator('.header-avatar')).toContainText('시');
    // 드롭다운 내용 (CSS 로 :hover/:focus-within 열림 — DOM 은 항상 존재)
    await expect(page.locator('.header-user-dropdown a[href="/mypage"]')).toContainText('마이페이지');
    await expect(page.locator('form.header-dropdown-logout button')).toContainText('로그아웃');
    // 비인증 로그인 아이콘은 없어야 함
    await expect(page.locator('a.header-icon-btn--primary[href="/login"]')).toHaveCount(0);
});
