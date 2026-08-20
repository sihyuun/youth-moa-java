import { test, expect } from '@playwright/test';
import { abortExternal } from '../helpers';

/**
 * F0e — 홈 prototype 정렬 E2E.
 *
 * 검증 대상: Hero + Quick Stats + 프로그램 4 + 공지 대표+3 + 공간 3 섹션이 렌더되고,
 * 카테고리 그리드와 HTMX Ping 데모 섹션이 완전히 제거됐음.
 */

test.beforeEach(async ({ page }) => {
    await abortExternal(page);
    // 260820: waitUntil:'commit' 은 HTML 파싱 전 반환이라 count() 계열이 0 을 잡는 flakiness 유발.
    // domcontentloaded 로 승격해 DOM 완성 후 검사 (toHaveCount·toBeVisible 은 자체 auto-wait 이나
    // await page.locator(...).count() 같은 직접 호출은 auto-wait 없음).
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await expect(page).toHaveTitle(/youth-moa/);
});

test('Hero 배너와 검색바가 표시된다', async ({ page }) => {
    await expect(page.locator('.hero-title')).toContainText('청년의 모든 기회를');
    await expect(page.locator('.hero-search-bar')).toBeVisible();
    await expect(page.locator('.hero-search-input')).toBeVisible();
});

test('Quick Stats 3개 지표가 표시된다', async ({ page }) => {
    const stats = page.locator('.quick-stats .quick-stat');
    await expect(stats).toHaveCount(3);
    await expect(page.locator('.quick-stats')).toContainText('모집중 프로그램');
    await expect(page.locator('.quick-stats')).toContainText('참여 청년센터');
    await expect(page.locator('.quick-stats')).toContainText('누적 참여자');
});

test('프로그램 섹션에 카드가 최대 4개 렌더된다 (비로그인)', async ({ page }) => {
    const cards = page.locator('.home-program-row .home-program-card');
    // toBeVisible 은 auto-wait 지원. beforeEach 승격 이후 재실행 시 안전.
    await expect(cards.first()).toBeVisible();
    const count = await cards.count();
    expect(count).toBeLessThanOrEqual(4);
    expect(count).toBeGreaterThan(0);
});

test('공지 섹션에 대표 + 서브 3건이 표시된다', async ({ page }) => {
    await expect(page.locator('.home-notice-section')).toBeVisible();
    await expect(page.locator('.home-notice-main')).toBeVisible();
    const subItems = page.locator('.home-notice-list .home-notice-item');
    await expect(subItems).toHaveCount(3);
});

test('공간 섹션 3개 카드', async ({ page }) => {
    const spaces = page.locator('.home-space-row .home-space-card');
    await expect(spaces).toHaveCount(3);
});

test('카테고리 그리드가 존재하지 않는다', async ({ page }) => {
    await expect(page.locator('.category-grid')).toHaveCount(0);
    await expect(page.locator('.category-card')).toHaveCount(0);
});

test('HTMX Ping 데모 섹션이 존재하지 않는다', async ({ page }) => {
    await expect(page.locator('#ping-result')).toHaveCount(0);
    await expect(page.locator('[hx-post="/api/ping"]')).toHaveCount(0);
});
