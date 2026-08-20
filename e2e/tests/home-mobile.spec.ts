import { test, expect } from '@playwright/test';
import { abortExternal } from '../helpers';

/**
 * 홈 화면 모바일 뷰포트 (375×812) 회귀 E2E.
 *
 * 배경: PR #152 (헤더 모바일 겹침 수정) 이후 사이트 헤더가 모바일에서 다르게 렌더된다.
 * 홈 페이지의 나머지 섹션이 좁은 뷰포트에서도 정상 렌더·정보 손실 없는지 회귀로 잡는다.
 *
 * 데스크톱 검증은 `home.spec.ts` 에서 담당. 이 spec 은 모바일 전용 관측만.
 */

test.use({ viewport: { width: 375, height: 812 } });

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await expect(page).toHaveTitle(/youth-moa/);
});

test('사이트 헤더 — 로고 한 줄 · nav 숨김 · 검색·로그인 아이콘 노출', async ({ page }) => {
    // #152 회귀 방지: 로고 텍스트가 3줄로 쪼개지지 않고 nav 3링크는 숨겨져야 함.
    const logo = page.locator('.header-logo-text');
    await expect(logo).toBeVisible();
    await expect(logo).toHaveCSS('white-space', 'nowrap');
    const nav = page.locator('.header-nav');
    await expect(nav).toBeHidden();
    // 우측 액션은 여전히 보여야 함
    await expect(page.locator('.header-icon-btn').first()).toBeVisible();
});

test('Hero 배너·검색바가 표시된다', async ({ page }) => {
    await expect(page.locator('.hero-title')).toContainText('청년의 모든 기회를');
    await expect(page.locator('.hero-search-bar')).toBeVisible();
    await expect(page.locator('.hero-search-input')).toBeVisible();
});

test('Quick Stats 3개 지표', async ({ page }) => {
    await expect(page.locator('.quick-stats .quick-stat')).toHaveCount(3);
});

test('프로그램 섹션 카드가 렌더된다 (최대 4)', async ({ page }) => {
    const cards = page.locator('.home-program-row .home-program-card');
    await expect(cards.first()).toBeVisible();
    const count = await cards.count();
    expect(count).toBeLessThanOrEqual(4);
    expect(count).toBeGreaterThan(0);
});

test('공지 섹션 대표 + 서브 3건', async ({ page }) => {
    await expect(page.locator('.home-notice-main')).toBeVisible();
    await expect(page.locator('.home-notice-list .home-notice-item')).toHaveCount(3);
});

test('공간 섹션 3개 카드', async ({ page }) => {
    await expect(page.locator('.home-space-row .home-space-card')).toHaveCount(3);
});
